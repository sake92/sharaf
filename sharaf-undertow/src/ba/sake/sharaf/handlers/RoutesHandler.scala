package ba.sake.sharaf.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.undertow.*

final class RoutesHandler private (routes: UndertowSharafRoutes, nextHandler: Option[HttpHandler]) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    given UndertowSharafRequest = UndertowSharafRequest.create(exchange)
    val reqParams = fillReqParams(exchange)
    val resOpt = routes.definition.lift(reqParams)
    resOpt match {
      case Some(res) => ResponseUtils.writeResponse(res, exchange)
      case None =>
        nextHandler match
          case Some(next) => next.handleRequest(exchange)
          case None       =>
            // will be catched by ExceptionHandler
            throw exceptions.NotFoundException("route")
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

object RoutesHandler:
  def apply(routes: UndertowSharafRoutes): RoutesHandler =
    new RoutesHandler(routes, None)

  def apply(routes: UndertowSharafRoutes, nextHandler: HttpHandler): RoutesHandler =
    new RoutesHandler(routes, Some(nextHandler))
