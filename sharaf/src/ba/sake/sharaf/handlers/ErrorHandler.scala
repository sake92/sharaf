package ba.sake.sharaf.handlers

import scala.util.control.NonFatal

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

import ba.sake.sharaf.*

class ErrorHandler(next: HttpHandler, errorMapper: ErrorMapper) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if (exchange.isInIoThread) {
      exchange.dispatch(this)
    } else {
      try {
        next.handleRequest(exchange)
      } catch {
        case NonFatal(e) if exchange.isResponseChannelAvailable =>
          val responseOpt = errorMapper.lift(e)
          responseOpt match {
            case Some(response) =>
              ResponseWritable.writeResponse(response, exchange)
            case None =>
              // if no error response match, just propagate.
              // will return 500
              throw e
          }
      }

    }
  }

}

object ErrorHandler {
  def apply(next: HttpHandler, errorMapper: ErrorMapper = ErrorMapper.default): ErrorHandler =
    new ErrorHandler(next, errorMapper)
}
