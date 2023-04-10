package ba.sake.sharaf.handlers

import scala.util.control.NonFatal
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

import ba.sake.tupson.*
import ba.sake.sharaf.*
import io.undertow.util.Headers
import java.net.URI
import org.typelevel.jawn.ast.*

final class ErrorHandler private (
    httpHandler: HttpHandler,
    errorMapper: ErrorMapper
) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = try {
    exchange.startBlocking()
    if (exchange.isInIoThread()) {
      exchange.dispatch(this)
    } else {
      httpHandler.handleRequest(exchange)
    }
  } catch {
    case NonFatal(e) =>
      if (exchange.isResponseChannelAvailable()) {

        val request = Request.fromHttpServerExchange(exchange)

        // TODO handle properly when multiple accepts..
        val acceptContentType = exchange.getRequestHeaders().get(Headers.ACCEPT)
        val response =
          if acceptContentType.getFirst() == "application/json" then {
            val mapper = errorMapper.orElse(ErrorMapper.json)
            val response = mapper((e, request))
            response.withHeader(Headers.CONTENT_TYPE_STRING, "application/json")
          } else {
            val mapper = errorMapper.orElse(ErrorMapper.default)
            val response = mapper((e, request))
            response.withHeader(Headers.CONTENT_TYPE_STRING, "text/plain")
          }

        val contentType = response.headers(Headers.CONTENT_TYPE_STRING).head
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType)

        exchange.setStatusCode(response.status)

        exchange.getResponseSender().send(response.body)
      }
  }

}

object ErrorHandler {
  def apply(httpHandler: HttpHandler, errorMapper: ErrorMapper = ErrorMapper.noop): ErrorHandler =
    new ErrorHandler(httpHandler, errorMapper)
}

/////////////
type ErrorMapper = PartialFunction[(Throwable, Request), Response]

object ErrorMapper {
  val default: ErrorMapper = (e, req) =>
    e match {
      case ex: ValidationException =>
        val fieldValidationErrors = ex.errors.mkString("[", "; ", "]")
        Response(s"Validation errors: $fieldValidationErrors").withStatus(400)
      case pe: ParsingException =>
        Response(e.getMessage()).withStatus(400)
      case te: TupsonException =>
        Response(e.getMessage()).withStatus(400)
    }

  val json: ErrorMapper = (e, req) =>
    e match {
      case ex: ValidationException =>
        val fieldValidationErrors = ex.errors.map(err => ValidationProblem(err.reason, err.name))
        val problemDetails = ProblemDetails(
          400,
          "Validation errors",
          invalidParams = fieldValidationErrors
        )
        Response.json(problemDetails).withStatus(400)
      case pe: ParsingException =>
        Response(e.getMessage()).withStatus(400)
      case te: TupsonException =>
        Response(e.getMessage()).withStatus(400)
    }

  val noop: ErrorMapper = {
    case _ if false => Response("") // by default no match
  }
}

/////////////
given JsonRW[URI] = new {

  override def write(value: URI): JValue = JString(value.toString)

  override def parse(jValue: JValue): URI = jValue match
    case JString(s) => URI.create(s)
    case _          => throw TupsonException(s"Invalid URI '$jValue'")

}

// https://www.rfc-editor.org/rfc/rfc7807#section-3.1
case class ProblemDetails(
    status: Int, // http status code
    title: String, // short summary
    detail: String = "",
    `type`: Option[URI] = None, // general error description URL
    instance: Option[URI] = None, // this particular error URL
    invalidParams: Seq[ValidationProblem] = Seq.empty
) derives JsonRW

case class ValidationProblem(
    reason: String,
    name: Option[String]
) derives JsonRW
