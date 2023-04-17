package ba.sake.sharaf.handlers

import scala.util.control.NonFatal
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

import ba.sake.tupson.*
import ba.sake.sharaf.*
import io.undertow.util.Headers
import java.net.URI
import org.typelevel.jawn.ast.*
import ba.sake.validation.FieldsValidationException

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
        val responseOpt =
          if acceptContentType.getFirst() == "application/json" then {
            val mapper = errorMapper.orElse(ErrorMapper.json)
            mapper.lift(e)
          } else {
            val mapper = errorMapper.orElse(ErrorMapper.default)
            mapper.lift(e)
          }

        responseOpt.foreach { response =>
          val contentType = response.headers(Headers.CONTENT_TYPE_STRING).head
          exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType)

          exchange.setStatusCode(response.status)

          exchange.getResponseSender().send(response.body)
        }
        // TODO if no error match, just propagate

      }
  }

}

object ErrorHandler {
  def apply(httpHandler: HttpHandler, errorMapper: ErrorMapper): ErrorHandler =
    new ErrorHandler(httpHandler, errorMapper)
}

/////////////
type ErrorMapper = PartialFunction[Throwable, Response]

object ErrorMapper {
  val default: ErrorMapper = {
    case e: FieldsValidationException =>
      val fieldValidationErrors = e.errors.mkString("[", "; ", "]")
      Response(s"Validation errors: $fieldValidationErrors").withStatus(400)
    case e: ParsingException =>
      Response(e.getMessage()).withStatus(400)
    case e: TupsonException =>
      Response(e.getMessage()).withStatus(400)
  }

  val json: ErrorMapper = {
    case e: FieldsValidationException =>
      val fieldValidationErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, Some(err.fieldValue.toString)))
      val problemDetails = ProblemDetails(400, "Validation errors", invalidArguments = fieldValidationErrors)
      Response.json(problemDetails).withStatus(400)
    case e: ParsingException =>
      val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
      val problemDetails = ProblemDetails(400, "JSON Parsing errors", invalidArguments = parsingErrors)
      Response.json(problemDetails).withStatus(400)
    case e: TupsonException =>
      Response.json(ProblemDetails(400, "JSON parsing error", e.getMessage)).withStatus(400)
    case e =>
      e.printStackTrace()
      Response.json(ProblemDetails(500, "Internal error", e.getMessage)).withStatus(400)
  }

}

/////////////
given JsonRW[URI] = new {

  override def write(value: URI): JValue = JString(value.toString)

  override def parse(path: String, jValue: JValue): URI = jValue match
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
    invalidArguments: Seq[ArgumentProblem] = Seq.empty
) derives JsonRW

case class ArgumentProblem(
    path: String,
    reason: String,
    value: Option[String]
) derives JsonRW
