package ba.sake.sharaf.jdkhttp

import scala.jdk.CollectionConverters.*
import com.sun.net.httpserver.HttpExchange
import ba.sake.sharaf.*

object ResponseUtils {

  def writeResponse(response: Response[?], exchange: HttpExchange): Unit = {
    val responseHeaders = exchange.getResponseHeaders

    val bodyContentHeaders = response.body.flatMap(response.rw.headers)
    bodyContentHeaders.foreach { case (name, values) =>
      responseHeaders.put(name.toString, values.asJava)
    }

    response.headerUpdates.updates.foreach {
      case HeaderUpdate.Set(name, values) =>
        responseHeaders.put(name.toString, values.asJava)
      case HeaderUpdate.Remove(name) =>
        responseHeaders.remove(name.toString)
    }

    response.cookieUpdates.updates.foreach { cookie =>
      val cookieValue = CookieUtils.toSetCookieHeader(cookie)
      val existingCookies = Option(responseHeaders.get("Set-Cookie")).map(_.asScala.toList).getOrElse(Nil)
      responseHeaders.put("Set-Cookie", (existingCookies :+ cookieValue).asJava)
    }

    response.body match {
      case None =>
        // -1 means no response body, mandatory for JDK HTTP server!
        exchange.sendResponseHeaders(response.status.code, -1)
      case Some(body) =>
        // ResponseWritable.write will write to the output stream
        // We need to know the content length beforehand for JDK HTTP server
        val bodyBytes = {
          val baos = new java.io.ByteArrayOutputStream()
          response.rw.write(body, baos)
          baos.toByteArray()
        }
        exchange.sendResponseHeaders(response.status.code, bodyBytes.length.toLong)
        val os = exchange.getResponseBody
        os.write(bodyBytes)
        os.close()
    }
  }

}
