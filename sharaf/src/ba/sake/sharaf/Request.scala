package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import ba.sake.tupson.*
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.validson.*
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormData as UFormData
import io.undertow.server.handlers.form.FormParserFactory
import io.undertow.util.HttpString

final class Request(
    private val ex: HttpServerExchange
) {

  /** Please use this with caution! */
  val underlyingHttpServerExchange: HttpServerExchange = ex

  /* QUERY */
  lazy val queryParamsMap: QueryStringMap =
    ex.getQueryParameters.asScala.toMap.map { (k, v) =>
      (k, v.asScala.toSeq)
    }

  def queryParams[T: Validator](using rw: QueryStringRW[T]): T =
    queryParamsMap.parseQueryStringMap.validateOrThrow

  /* BODY */
  lazy val bodyString: String =
    new String(ex.getInputStream.readAllBytes(), StandardCharsets.UTF_8)

  def bodyJson[T: Validator](using rw: JsonRW[T]): T =
    bodyString.parseJson[T].validateOrThrow

  def bodyForm[T <: Product: Validator](using rw: FormDataRW[T]): T = {
    // returns null if content-type is not suitable
    Option(FormParserFactory.builder.build.createParser(ex)) match
      case None => throw new SharafException("The specified content type is not supported")
      case Some(parser) =>
        val uFormData = parser.parseBlocking()
        val formData = Request.undertowFormData2Formson(uFormData)
        rw.parse("", formData).validateOrThrow
  }

  /* HEADERS */
  def headers: Map[HttpString, Seq[String]] = {
    val hMap = ex.getRequestHeaders
    hMap.getHeaderNames.asScala.map { name =>
      name -> hMap.get(name).asScala.toSeq
    }.toMap
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
