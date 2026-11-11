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
          case None          => throw e
        }
    } finally req.closeUploads()

  private def fillReqParams(exchange: HttpExchange): RequestParams = {
    val method = HttpMethod.valueOf(exchange.getRequestMethod)
    val path = RequestDecoding.path(exchange.getRequestURI.getRawPath)
    (method = method, path = path)
  }
}
