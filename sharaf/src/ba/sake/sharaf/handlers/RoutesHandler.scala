package ba.sake.sharaf.handlers

import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
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

      given Request = Request.create(exchange)

      val reqParams = fillReqParams(exchange)

      val resOpt = routes.lift(reqParams)

      resOpt match
        case Some(res) => writeResponse(res, exchange)
        case None =>
          val acceptContentType = exchange.getRequestHeaders.get(Headers.ACCEPT)
          if acceptContentType.getFirst == "application/json" then {
            val problemDetails = ProblemDetails(404, "Not Found")
            writeResponse(Response.withBody(problemDetails).withStatus(404), exchange)
          } else writeResponse(Response.withBody("Not Found").withStatus(404), exchange)

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

  private def writeResponse(response: Response[?], exchange: HttpServerExchange): Unit = {
    // headers
    val allHeaders = response.rw.headers ++ response.headers
    allHeaders.foreach { case (name, values) =>
      exchange.getResponseHeaders.putAll(new HttpString(name), values.asJava)
    }
    // status code
    exchange.setStatusCode(response.status)
    // body
    response.rw.write(response.body, exchange)
  }

}

object RoutesHandler {
  def apply(routes: Routes): RoutesHandler =
    new RoutesHandler(routes)
}
