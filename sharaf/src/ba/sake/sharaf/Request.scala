package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import scala.collection.mutable
import scala.collection.immutable.SeqMap
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormData as UFormData
import io.undertow.server.handlers.form.FormParserFactory
import io.undertow.util.HttpString
import ba.sake.tupson.*
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.validson.*
import org.typelevel.jawn.ast.JValue
import ba.sake.sharaf.exceptions.*

final class Request private (
    private val undertowExchange: HttpServerExchange
) {

  /** Please use this with caution! */
  val underlyingHttpServerExchange: HttpServerExchange = undertowExchange

  /* QUERY */
  lazy val queryParamsRaw: QueryStringMap =
    undertowExchange.getQueryParameters.asScala.toMap.map { (k, v) =>
      (k, v.asScala.toSeq)
    }

  // must be a Product (case class)
  def queryParams[T <: Product: QueryStringRW]: T =
    try queryParamsRaw.parseQueryStringMap
    catch case e: QuersonException => throw RequestHandlingException(e)

  def queryParamsValidated[T <: Product: QueryStringRW: Validator]: T =
    try queryParams[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  /* BODY */
  private val formBodyParserFactory = locally {
    val parserFactoryBuilder = FormParserFactory.builder
    parserFactoryBuilder.setDefaultCharset("utf-8")
    parserFactoryBuilder.build
  }

  lazy val bodyString: String =
    String(undertowExchange.getInputStream.readAllBytes(), StandardCharsets.UTF_8)

  // JSON
  def bodyJsonRaw: JValue = bodyJson[JValue]

  def bodyJson[T: JsonRW]: T =
    try bodyString.parseJson[T]
    catch case e: TupsonException => throw RequestHandlingException(e)

  def bodyJsonValidated[T: JsonRW: Validator]: T =
    try bodyJson[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  // FORM
  def bodyFormRaw: FormDataMap =
    // createParser returns null if content-type is not suitable
    val parser = formBodyParserFactory.createParser(undertowExchange)
    Option(parser) match
      case None => throw SharafException("The specified content type is not supported")
      case Some(parser) =>
        val uFormData = parser.parseBlocking()
        Request.undertowFormData2FormsonMap(uFormData)

  // must be a Product (case class)
  def bodyForm[T <: Product: FormDataRW]: T =
    try bodyFormRaw.parseFormDataMap[T]
    catch case e: FormsonException => throw RequestHandlingException(e)

  def bodyFormValidated[T <: Product: FormDataRW: Validator]: T =
    try bodyForm[T].validateOrThrow
    catch case e: ValidsonException => throw RequestHandlingException(e)

  /* HEADERS */
  def headers: Map[HttpString, Seq[String]] =
    val hMap = undertowExchange.getRequestHeaders
    hMap.getHeaderNames.asScala.map { name =>
      name -> hMap.get(name).asScala.toSeq
    }.toMap
    
  def cookies: Seq[Cookie] = 
    undertowExchange.requestCookies().asScala.map(Cookie.fromUndertow).toSeq

}

object Request {
  def current(using req: Request): Request = req

  private[sharaf] def create(undertowExchange: HttpServerExchange): Request =
    Request(undertowExchange)

  private[sharaf] def undertowFormData2FormsonMap(uFormData: UFormData): FormDataMap = {
    val map = mutable.LinkedHashMap.empty[String, Seq[FormValue]]
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
    SeqMap.from(map)
  }
}
