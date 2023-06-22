package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import ba.sake.tupson.*
import ba.sake.formson.*
import ba.sake.querson.*
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormData as UFormData
import io.undertow.server.handlers.form.FormParserFactory

final class Request(
    private val ex: HttpServerExchange
) {

  /** Please use this with caution! */
  val underlyingHttpServerExchange: HttpServerExchange = ex

  lazy val bodyString: String =
    new String(ex.getInputStream.readAllBytes(), StandardCharsets.UTF_8)

  def bodyJson[T](using rw: JsonRW[T]): T =
    bodyString.parseJson[T]

  def bodyForm[T <: Product](using rw: FormDataRW[T]): T = {
    // TODO morebit null WTFFF provjerit jel ima forme uopće, možda fali header i to..
    val parser = FormParserFactory.builder.build.createParser(ex)
    val uFormData = parser.parseBlocking()

    val formData = Request.undertowFormData2Formson(uFormData)
    rw.parse("", formData)
  }

  def queryParams[T](using rw: QueryStringRW[T]): T = {
    val queryParams: QueryStringMap = ex.getQueryParameters.asScala.toMap.map { (k, v) =>
      (k, v.asScala.toSeq)
    }
    queryParams.parseQueryStringMap
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
        if value.isFileItem then
          val fileItem = value.getFileItem
          if fileItem.isInMemory then
            val byteArray = Array.ofDim[Byte](fileItem.getInputStream.available)
            fileItem.getInputStream.read(byteArray)
            FormValue.ByteArray(byteArray)
          else FormValue.File(fileItem.getFile)
        else FormValue.Str(value.getValue)
      }
      map += (key -> formValues.toSeq)
    }

    parseFDMap(map.toMap)
  }
}
