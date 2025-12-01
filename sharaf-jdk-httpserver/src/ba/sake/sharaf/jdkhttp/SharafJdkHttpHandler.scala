package ba.sake.sharaf.jdkhttp

import com.sun.net.httpserver.{HttpHandler, HttpExchange}
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.exceptions.NotFoundException

final class SharafJdkHttpHandler(sharafHandler: SharafHandler, next: Option[HttpHandler] = None) extends HttpHandler {

  override def handle(exchange: HttpExchange): Unit =
    val reqParams = fillReqParams(exchange)
    val req = JdkHttpServerSharafRequest.create(exchange)
    val requestContext = RequestContext(reqParams, req)
    try {
      val res = sharafHandler.handle(requestContext)
      ResponseUtils.writeResponse(res, exchange)
    } catch {
      case e: NotFoundException =>
        next match {
          case Some(handler) => handler.handle(exchange)
          case None => throw e
        }
    }

  private def fillReqParams(exchange: HttpExchange): RequestParams = {
    val method = HttpMethod.valueOf(exchange.getRequestMethod)
    val relPath =
      val path = exchange.getRequestURI.getPath
      if path.startsWith("/") then path.drop(1) else path
    val pathSegments = relPath.split("/")
    val path =
      if pathSegments.size == 1 && pathSegments.head == ""
      then Path()
      else Path(pathSegments*)
    (method, path)
  }
}
