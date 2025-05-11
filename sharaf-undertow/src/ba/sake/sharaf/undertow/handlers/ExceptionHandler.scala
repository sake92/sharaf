package ba.sake.sharaf.undertow.handlers

import scala.util.control.NonFatal
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.*
import ba.sake.sharaf.exceptions.ExceptionMapper

final class ExceptionHandler private (next: HttpHandler, exceptionMapper: ExceptionMapper) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit =
    try next.handleRequest(exchange)
    catch {
      case NonFatal(e) if exchange.isResponseChannelAvailable =>
        val responseOpt = exceptionMapper.lift(e)
        responseOpt match {
          case Some(response) =>
            ResponseUtils.writeResponse(response, exchange)
          case None =>
            // if no error response match, just propagate.
            // will return 500
            throw e
        }
    }

}

object ExceptionHandler {
  def apply(next: HttpHandler, exceptionMapper: ExceptionMapper = ExceptionMapper.default): ExceptionHandler =
    new ExceptionHandler(next, exceptionMapper)
}
