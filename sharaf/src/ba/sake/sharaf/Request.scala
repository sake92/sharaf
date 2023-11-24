package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*

import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormData as UFormData
import io.undertow.server.handlers.form.FormParserFactory
import io.undertow.util.HttpString

import ba.sake.tupson, tupson.*
import ba.sake.formson, formson.*
import ba.sake.querson, querson.*
import ba.sake.validson, validson.*

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

  def queryParams[T <: Product: QueryStringRW]: T =
    try queryParamsMap.parseQueryStringMap
    catch case e: querson.ParsingException => throw RequestHandlingException(e)

  def queryParamsValidated[T <: Product: QueryStringRW: Validator]: T =
    try queryParams[T].validateOrThrow
    catch case e: validson.ValidationException => throw RequestHandlingException(e)

  /* BODY */
  private val formBodyParserFactory = locally {
    val parserFactoryBuilder = FormParserFactory.builder
    parserFactoryBuilder.setDefaultCharset("utf-8")
    parserFactoryBuilder.build
  }

  lazy val bodyString: String =
    String(ex.getInputStream.readAllBytes(), StandardCharsets.UTF_8)

  // JSON
  def bodyJson[T: JsonRW]: T =
    try bodyString.parseJson[T]
    catch case e: tupson.ParsingException => throw RequestHandlingException(e)

  def bodyJsonValidated[T: JsonRW: Validator]: T =
    try bodyJson[T].validateOrThrow
    catch case e: validson.ValidationException => throw RequestHandlingException(e)

  // FORM
  def bodyForm[T <: Product: FormDataRW]: T =
    // createParser returns null if content-type is not suitable
    val parser = formBodyParserFactory.createParser(ex)
    Option(parser) match
      case None => throw SharafException("The specified content type is not supported")
      case Some(parser) =>
        val uFormData = parser.parseBlocking()
        val formDataMap = Request.undertowFormData2FormsonMap(uFormData)
        try formDataMap.parseFormDataMap[T]
        catch case e: formson.ParsingException => throw RequestHandlingException(e)

  def bodyFormValidated[T <: Product: FormDataRW: Validator]: T =
    try bodyForm[T].validateOrThrow
    catch case e: validson.ValidationException => throw RequestHandlingException(e)

  /* HEADERS */
  def headers: Map[HttpString, Seq[String]] =
    val hMap = ex.getRequestHeaders
    hMap.getHeaderNames.asScala.map { name =>
      name -> hMap.get(name).asScala.toSeq
    }.toMap

}

object Request {
  def current(using req: Request): Request = req

  private[sharaf] def create(ex: HttpServerExchange): Request =
    Request(ex)

  private[sharaf] def undertowFormData2FormsonMap(uFormData: UFormData): FormDataMap = {
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
    map.toMap
  }
}
