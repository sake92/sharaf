package ba.sake.sharaf.undertow

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.undertow.*
import ba.sake.sharaf.exceptions.NotFoundException

final class SharafUndertowHandler(sharafHandler: SharafHandler, next: Option[HttpHandler] = None) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit =
    val reqParams = fillReqParams(exchange)
    val req = UndertowSharafRequest.create(exchange)
    val requestContext = RequestContext(reqParams, req)
    try {
      val res = sharafHandler.handle(requestContext)
      ResponseUtils.writeResponse(res, exchange)
    } catch {
      case e: NotFoundException =>
        next match {
          case Some(handler) => handler.handleRequest(exchange)
          case None          => throw e
        }
    }

  private def fillReqParams(exchange: HttpServerExchange): RequestParams = {
    val method = HttpMethod.valueOf(exchange.getRequestMethod.toString)
    val relPath =
      if exchange.getRelativePath.startsWith("/") then exchange.getRelativePath.drop(1)
      else exchange.getRelativePath
    val pathSegments = relPath.split("/")
    val path =
      if pathSegments.size == 1 && pathSegments.head == ""
      then Path()
      else Path(pathSegments*)
    (method, path)
  }
}
