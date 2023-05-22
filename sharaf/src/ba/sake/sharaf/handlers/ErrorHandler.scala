package ba.sake.sharaf.handlers

import scala.util.control.NonFatal
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

import ba.sake.tupson.*
import ba.sake.formson.*
import ba.sake.sharaf.*
import io.undertow.util.Headers
import java.net.URI
import org.typelevel.jawn.ast.*
import ba.sake.validation.FieldsValidationException

final class ErrorHandler private (
    httpHandler: HttpHandler,
    errorMapper: ErrorMapper[String]
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

        val request = Request.create(exchange)

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

         // exchange.getResponseSender().send(response.body)
        }

        // if no error match, just propagate
        throw e
      }
  }

}

object ErrorHandler {
  // TODO accept multiple errormappers, one per content type ?
  def apply(httpHandler: HttpHandler): ErrorHandler =
   apply(httpHandler, { case _ if false => Response("should not happen") })
  def apply(httpHandler: HttpHandler, errorMapper: ErrorMapper[String]): ErrorHandler =
    new ErrorHandler(httpHandler, errorMapper)
}

/////////////
type ErrorMapper[T] = PartialFunction[Throwable, Response[T]]

object ErrorMapper {
  val default: ErrorMapper[String] = {
    case e: NotFoundException =>
      Response(e.getMessage).withStatus(404)
    case e: FieldsValidationException =>
      val fieldValidationErrors = e.errors.mkString("[", "; ", "]")
      Response(s"Validation errors: $fieldValidationErrors").withStatus(400)
    // json
    case e: ParsingException =>
      Response(e.getMessage()).withStatus(400)
    case e: TupsonException =>
      Response(e.getMessage()).withStatus(400)
    // form
    case e: FormsonException =>
      Response(e.getMessage()).withStatus(400)
  }

  val json: ErrorMapper[ProblemDetails] = {
    case e: NotFoundException =>
      val problemDetails = ProblemDetails(400, "Not Found", e.getMessage)
      Response.withBody(problemDetails).withStatus(404)
    case e: FieldsValidationException =>
      val fieldValidationErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, Some(err.value.toString)))
      val problemDetails = ProblemDetails(400, "Validation errors", invalidArguments = fieldValidationErrors)
      Response.withBody(problemDetails).withStatus(400)
    // json
    case e: ParsingException =>
      val parsingErrors = e.errors.map(err => ArgumentProblem(err.path, err.msg, err.value.map(_.toString)))
      val problemDetails = ProblemDetails(400, "JSON Parsing errors", invalidArguments = parsingErrors)
      Response.withBody(problemDetails).withStatus(400)
    case e: TupsonException =>
      Response.withBody(ProblemDetails(400, "JSON parsing error", e.getMessage)).withStatus(400)
    // form
    case e: FormsonException =>
      Response.withBody(ProblemDetails(400, "Form parsing error", e.getMessage)).withStatus(400)
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
