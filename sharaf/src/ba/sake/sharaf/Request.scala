package ba.sake.sharaf

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import ba.sake.tupson.*
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormData
import io.undertow.server.handlers.form.FormParserFactory

final class Request(
    private val ex: HttpServerExchange
) {
  lazy val bodyString: String =
    new String(ex.getInputStream.readAllBytes(), StandardCharsets.UTF_8)

  def bodyJson[T](using rw: JsonRW[T]): T =
    bodyString.parseJson[T]

  def bodyForm[T](using ffd: FromFormData[T]): T = {
    val parser = FormParserFactory.builder.build.createParser(ex)
    val formData = parser.parseBlocking()
    val formDataMap = formData.iterator.asScala.map { key =>
      key -> formData.get(key).asScala.toSeq
    }.toMap
    ffd.bind(formDataMap)
  }
}

object Request {
  def current(using req: Request): Request = req

  private[sharaf] def create(ex: HttpServerExchange): Request =
    Request(ex)
}
