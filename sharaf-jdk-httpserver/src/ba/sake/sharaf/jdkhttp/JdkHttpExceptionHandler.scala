package ba.sake.sharaf.jdkhttp

import scala.util.control.NonFatal
import com.sun.net.httpserver.{HttpHandler, HttpExchange}
import sttp.model.*

import ba.sake.sharaf.Response
import ba.sake.sharaf.exceptions.ExceptionMapper

// TODO remove
final class JdkHttpExceptionHandler(exceptionMapper: ExceptionMapper, next: HttpHandler) extends HttpHandler {

  override def handle(exchange: HttpExchange): Unit =
    try next.handle(exchange)
    catch {
      case NonFatal(e) =>
        val responseOpt = exceptionMapper.lift(e)
        responseOpt match {
          case Some(response) =>
            ResponseUtils.writeResponse(response, exchange)
          case None =>
            // if no error response match, just propagate.
            // will return 500
            e.printStackTrace()
            ResponseUtils.writeResponse(Response.withStatus(StatusCode.InternalServerError), exchange)
        }
    }

}
