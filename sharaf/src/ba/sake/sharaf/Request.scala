package ba.sake.sharaf

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import ba.sake.tupson.*
import ba.sake.formson.*
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.{FormData => UFormData}
import io.undertow.server.handlers.form.FormParserFactory

final class Request(
    private val ex: HttpServerExchange
) {
  lazy val bodyString: String =
    new String(ex.getInputStream.readAllBytes(), StandardCharsets.UTF_8)

  def bodyJson[T](using rw: JsonRW[T]): T =
    bodyString.parseJson[T]

  def bodyForm[T <: Product](using ffd: FromFormData[T]): T = {
    val parser = FormParserFactory.builder.build.createParser(ex)
    val uFormData = parser.parseBlocking()

    val formData = Request.undertowFormData2Formson(uFormData)
    ffd.bind("", formData)
  }
}

object Request {
  def current(using req: Request): Request = req

  private[sharaf] def create(ex: HttpServerExchange): Request =
    Request(ex)

  private[sharaf] def undertowFormData2Formson(uFormData: UFormData): FormData = {
    val map = scala.collection.mutable.Map.empty[String, Seq[FormValue]]
    uFormData.forEach { key =>
      val values = uFormData.get(key).asScala
      val formValues = values.map { value =>
        if value.isFileItem then FormValue.File(value.getFileItem.getFile)
        else FormValue.Str(value.getValue)
      }
      map += (key -> formValues.toSeq)
    }

    parseForm(map.toMap)
  }
}
