package ba.sake.sharaf

import java.io.InputStream
import java.nio.charset.StandardCharsets

import ba.sake.tupson.*
import io.undertow.server.HttpServerExchange

case class Request(
    is: InputStream
    // TODO headers
) {
  lazy val bodyString: String =
    new String(is.readAllBytes(), StandardCharsets.UTF_8)

  def bodyJson[T](using rw: JsonRW[T]): T =
    bodyString.parseJson[T]
}

object Request {
  def current(using req: Request): Request = req

  private[sharaf] def fromHttpServerExchange(ex: HttpServerExchange): Request =
    Request(ex.getInputStream)
}
