package ba.sake.sharaf.handlers

import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.util.Headers
import io.undertow.util.StatusCodes

import ba.sake.sharaf.*

private[sharaf] final class RoutesHandler(routes: Routes) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if (exchange.isInIoThread()) {
      exchange.dispatch(this)
    } else {

      given Request(exchange.getInputStream)

      val reqParams = fillReqParams(exchange)
      val response = routes.applyOrElse(reqParams, _ => Response("Not Founddd")) // TODO

      val contentType = response.headers
        .get(Headers.CONTENT_TYPE_STRING)
        .map(_.head)
        .getOrElse("text/plain")
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType)

      exchange.setResponseCode(response.status)

      exchange.getResponseSender().send(response.body)
    }
  }

  private def fillReqParams(exchange: HttpServerExchange): RequestParams = {
    val relPath =
      if exchange.getRelativePath.startsWith("/")
      then exchange.getRelativePath.drop(1)
      else exchange.getRelativePath
    val pathSegments = relPath.split("/")
    val path = Path(pathSegments*)

    val queryParams = exchange.getQueryParameters().asScala.toSeq.map {
      (k, v) => (k, v.asScala.toSeq)
    }
    val queryString = new QueryString(queryParams)

    (exchange.getRequestMethod(), path, queryString)
  }

}
