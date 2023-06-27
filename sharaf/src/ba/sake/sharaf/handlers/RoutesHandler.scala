package ba.sake.sharaf.handlers

import scala.util.control.NonFatal

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

import ba.sake.sharaf.*

final class RoutesHandler private (routes: Routes, errorMapper: ErrorMapper) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if (exchange.isInIoThread) {
      exchange.dispatch(this)
    } else {

      val request = Request.create(exchange)
      given Request = request

      val reqParams = fillReqParams(exchange)

      try {

        val resOpt = routes.lift(reqParams)

        // if no match, a 500 will be returned by Undertow
        resOpt match {
          case Some(res) => ResponseWritable.writeResponse(res, exchange)
          case None      => throw NotFoundException("")
        }
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

  private def fillReqParams(exchange: HttpServerExchange): RequestParams = {
    val relPath =
      if exchange.getRelativePath.startsWith("/") then exchange.getRelativePath.drop(1)
      else exchange.getRelativePath
    val pathSegments = relPath.split("/")
    val path = Path(pathSegments*)

    (exchange.getRequestMethod, path)
  }

}

object RoutesHandler {
  def apply(routes: Routes, errorMapper: ErrorMapper = ErrorMapper.default): RoutesHandler =
    new RoutesHandler(routes, errorMapper)
}
