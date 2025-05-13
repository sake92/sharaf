package ba.sake.sharaf.helidon

import scala.jdk.CollectionConverters.*
import io.helidon.http.*
import io.helidon.webserver.http.ServerResponse
import ba.sake.sharaf.*

object ResponseUtils {

  def writeResponse(response: Response[?], helidonRes: ServerResponse): Unit = {
    val bodyContentHeaders = response.body.flatMap(response.rw.headers)
    bodyContentHeaders.foreach { case (name, values) =>
      val helidonHeaderName = HeaderNames.create(name.toString)
      helidonRes.headers().set(helidonHeaderName, values.asJava)
    }
    response.headerUpdates.updates.foreach {
      case HeaderUpdate.Set(name, values) =>
        val helidonHeaderName = HeaderNames.create(name.toString)
        helidonRes.headers().set(helidonHeaderName, values.asJava)
      case HeaderUpdate.Remove(name) =>
        val helidonHeaderName = HeaderNames.create(name.toString)
        helidonRes.headers().remove(helidonHeaderName)
    }
    /* TODO
    response.cookieUpdates.updates.foreach { cookie =>
      exchange.setResponseCookie(undertow.CookieUtils.toUndertow(cookie))
    }
     */

    helidonRes.status(response.status.code)
    response.body.foreach(b => response.rw.write(b, helidonRes.outputStream()))
  }
}
