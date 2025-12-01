package ba.sake.sharaf.undertow

import scala.util.control.NonFatal
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import ba.sake.sharaf.exceptions.ExceptionMapper

// TODO figure out how to remove this one :/
final class UndertowExceptionHandler(exceptionMapper: ExceptionMapper, next: HttpHandler) extends HttpHandler {

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
