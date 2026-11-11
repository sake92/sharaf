package ba.sake.sharaf.undertow

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.undertow.*
import ba.sake.sharaf.exceptions.NotFoundException

final class SharafUndertowHandler(sharafHandler: SharafHandler, notFoundHandler: Option[HttpHandler] = None)
    extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit =
    val reqParams = fillReqParams(exchange)
    val req = UndertowSharafRequest.create(exchange)
    val requestContext = RequestContext(reqParams, req)
    try {
      val res = sharafHandler.handle(requestContext)
      ResponseUtils.writeResponse(res, exchange)
      if !exchange.isPersistent && res.status.code == 413 then exchange.getOutputStream.flush()
    } catch {
      case e: NotFoundException =>
        notFoundHandler match {
          case Some(handler) => handler.handleRequest(exchange)
          case None          => throw e
        }
    } finally req.closeUploads()

  private def fillReqParams(exchange: HttpServerExchange): RequestParams = {
    val method = HttpMethod.valueOf(exchange.getRequestMethod.toString)
    val rawPath = java.net.URI.create(exchange.getRequestURI).getRawPath
    val fullPath = RequestDecoding.path(rawPath)
    // Undertow prefix handlers have already consumed this many path segments.
    val resolvedSegments = exchange.getResolvedPath.split("/").count(_.nonEmpty)
    val path = Path(fullPath.segments.drop(resolvedSegments)*)
    (method = method, path = path)
  }
}
