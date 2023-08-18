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

      // if no match, a 500 will be returned by Undertow
      resOpt match {
        case Some(res) => ResponseWritable.writeResponse(res, exchange)
        case None      => throw NotFoundException("")
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
  def apply(routes: Routes): RoutesHandler =
    new RoutesHandler(routes)
}
