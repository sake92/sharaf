package ba.sake.sharaf.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

import ba.sake.sharaf.*

final class RoutesHandler private (routes: Routes) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if (exchange.isInIoThread) {
      exchange.dispatch(this)
    } else {

      val request = Request.create(exchange)

      given Request = request

      val reqParams = fillReqParams(exchange)

      val resOpt = routes.lift(reqParams)

      resOpt match {
        case Some(res) => ResponseWritable.writeResponse(res, exchange)
        case None      =>
          // will be catched by ErrorMapper
          throw NotFoundException("route not found")
      }
    }
  }

  private def fillReqParams(exchange: HttpServerExchange): RequestParams = {
    val relPath =
      if exchange.getRelativePath.startsWith("/") then exchange.getRelativePath.drop(1)
      else exchange.getRelativePath
    val pathSegments = relPath.split("/")

    val path =
      if pathSegments.size == 1 && pathSegments.head == ""
      then Path()
      else Path(pathSegments*)

    (exchange.getRequestMethod, path)
  }

}

object RoutesHandler {
  def apply(routes: Routes): RoutesHandler =
    new RoutesHandler(routes)
}
