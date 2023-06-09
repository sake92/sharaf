package ba.sake.sharaf.handlers

import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString
import io.undertow.util.StatusCodes

import ba.sake.sharaf.*

final class RoutesHandler private (routes: Routes, errorMapper: ErrorMapper[String] = ErrorMapper.empty)
    extends HttpHandler {

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    exchange.startBlocking()
    if (exchange.isInIoThread) {
      exchange.dispatch(this)
    } else {

      try {
        given Request = Request.create(exchange)

        val reqParams = fillReqParams(exchange)
        val resOpt = routes.lift(reqParams)

        // if no match, a 500 will be returned by Undertow
        resOpt.foreach { res =>
          ResponseWritable.writeResponse(res, exchange)
        }
      } catch {
        case NonFatal(e) if exchange.isResponseChannelAvailable =>
          // TODO handle properly when multiple accepts..
          val acceptContentType = exchange.getRequestHeaders().get(Headers.ACCEPT)
          val responseOpt =
            if acceptContentType.getFirst() == "application/json" then {
              val mapper = errorMapper.orElse(ErrorMapper.json)
              mapper.lift(e)
            } else {
              val mapper = errorMapper.orElse(ErrorMapper.default)
              mapper.lift(e)
            }

          responseOpt match {
            case Some(response) => ResponseWritable.writeResponse(response, exchange)
            case None           =>
              // if no error response match, just propagate.
              // will return 500
              throw e
          }
      }

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
