package ba.sake.sharaf.helidon

import scala.jdk.CollectionConverters.*
import io.helidon.http.*
import io.helidon.webserver.http.ServerResponse
import ba.sake.sharaf.*

object ResponseUtils {

  def writeResponse(response: Response[?], helidonRes: ServerResponse): Unit = {
    val bodyContentHeaders = response.body.toSeq.flatMap(response.rw.headers)
    response.headerUpdates.resolve(bodyContentHeaders).foreach {
      case HeaderUpdate.Set(name, values) =>
        val helidonHeaderName = HeaderNames.create(name.toString)
        helidonRes.headers().set(helidonHeaderName, values.asJava)
      case HeaderUpdate.Remove(name) =>
        val helidonHeaderName = HeaderNames.create(name.toString)
        helidonRes.headers().remove(helidonHeaderName)
    }
    response.cookieUpdates.updates.foreach { cookie =>
      helidonRes.headers().add(HeaderNames.SET_COOKIE, CookieHeaders.render(cookie))
    }

    helidonRes.status(response.status.code)
    response.body match {
      case Some(body) => response.rw.write(body, helidonRes.outputStream())
      case None       => helidonRes.send()
    }
  }
}
