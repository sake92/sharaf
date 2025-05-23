package ba.sake.sharaf.undertow

import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString as UndertowHttpString
import ba.sake.sharaf.*

object ResponseUtils {

  def writeResponse(response: Response[?], exchange: HttpServerExchange): Unit = {
    val bodyContentHeaders = response.body.flatMap(response.rw.headers)
    bodyContentHeaders.foreach { case (name, values) =>
      val undertowHttpString = UndertowHttpString(name.toString)
      exchange.getResponseHeaders.putAll(undertowHttpString, values.asJava)
    }
    response.headerUpdates.updates.foreach {
      case HeaderUpdate.Set(name, values) =>
        val undertowHttpString = UndertowHttpString(name.toString)
        exchange.getResponseHeaders.putAll(undertowHttpString, values.asJava)
      case HeaderUpdate.Remove(name) =>
        val undertowHttpString = UndertowHttpString(name.toString)
        exchange.getResponseHeaders.remove(undertowHttpString)
    }

    response.cookieUpdates.updates.foreach { cookie =>
      exchange.setResponseCookie(undertow.CookieUtils.toUndertow(cookie))
    }

    exchange.setStatusCode(response.status.code)
    response.body.foreach(b => response.rw.write(b, exchange.getOutputStream))
  }

}
