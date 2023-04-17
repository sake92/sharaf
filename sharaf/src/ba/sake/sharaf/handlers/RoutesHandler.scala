package ba.sake.sharaf.handlers

import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.util.Headers
import io.undertow.util.StatusCodes

import ba.sake.sharaf.*
import io.undertow.util.HttpString

final class RoutesHandler private (routes: Routes) extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if (exchange.isInIoThread) {
      exchange.dispatch(this)
    } else {

      //exchange.getAttachment()

      given Request = Request.fromHttpServerExchange(exchange)

      val reqParams = fillReqParams(exchange)
      val response = routes.applyOrElse(
        reqParams,
        _ => {
          // TODO handle properly when multiple accepts..
          val acceptContentType = exchange.getRequestHeaders.get(Headers.ACCEPT)
          if acceptContentType.getFirst == "application/json" then {
            val problemDetails = ProblemDetails(404, "Not Found")
            Response.json(problemDetails).withStatus(404)
          } else Response("Not Found").withStatus(404)
        }
      )

      val contentType = response.headers
        .get(Headers.CONTENT_TYPE_STRING)
        .map(_.head)
        .getOrElse("text/plain")
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, contentType)

      exchange.setStatusCode(response.status)

      // TODO REMOVE
      response.headers.foreach { case (name, values) =>
        exchange.getResponseHeaders.putAll(new HttpString(name), values.asJava)
      }
      exchange.getResponseHeaders.put(new HttpString("Access-Control-Allow-Origin"), "*")

      // nothing after this line is applied!
      exchange.getResponseSender.send(response.body)
    }
  }

  private def fillReqParams(exchange: HttpServerExchange): RequestParams = {
    val relPath =
      if exchange.getRelativePath.startsWith("/") then exchange.getRelativePath.drop(1)
      else exchange.getRelativePath
    val pathSegments = relPath.split("/")
    val path = Path(pathSegments*)

    val queryParams = exchange.getQueryParameters.asScala.toMap.map { (k, v) =>
      (k, v.asScala.toSeq)
    }
    val queryString = new QueryString(queryParams)

    (exchange.getRequestMethod, path, queryString)
  }

}

object RoutesHandler {
  def apply(routes: Routes): RoutesHandler =
    new RoutesHandler(routes)
}
