package ba.sake.sharaf.handlers

import scala.util.control.NonFatal
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

import ba.sake.tupson.*
import ba.sake.sharaf.*
import io.undertow.util.Headers

type ErrorMapper = PartialFunction[(Throwable, Request), Response]

object ErrorMapper {
  // TODO if json ..
  val default: ErrorMapper = (e, req) =>
    e match {
      case _: ValidationException =>
        Response(e.getMessage()).withStatus(400)
      case pe: ParsingException => // TODO ErrorMessage klasa.. fino JSON
        Response(e.getMessage()).withStatus(400)
      case te: TupsonException =>
        Response(e.getMessage()).withStatus(400)
    }

  val noop: ErrorMapper = {
    case _ if false => Response("") // by default no match
  }
}

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

        val mapper = errorMapper.orElse(ErrorMapper.default)
        val req = Request.fromHttpServerExchange(exchange)
        val response = mapper((e, req))

        val contentType = response.headers
          .get(Headers.CONTENT_TYPE_STRING)
          .map(_.head)
          .getOrElse("text/plain")
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType)

        exchange.setStatusCode(response.status)

        exchange.getResponseSender().send(response.body)
      }
  }

}

object ErrorHandler {
  def apply(httpHandler: HttpHandler, errorMapper: ErrorMapper): ErrorHandler =
    new ErrorHandler(httpHandler, errorMapper)
}
