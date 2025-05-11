package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import scala.collection.mutable
import scala.collection.immutable.SeqMap
import io.undertow.server.HttpServerExchange as UHttpServerExchange
import io.undertow.server.handlers.form.FormData as UFormData
import io.undertow.server.handlers.form.FormParserFactory
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.exceptions.*

final class UndertowSharafRequest  (
    val underlyingHttpServerExchange: UHttpServerExchange
) extends Request {

  /*** HEADERS ***/
  def headers: Map[HttpString, Seq[String]] =
    val hMap = underlyingHttpServerExchange.getRequestHeaders
    hMap.getHeaderNames.asScala.map { name =>
      HttpString(name.toString) -> hMap.get(name).asScala.toSeq
    }.toMap

  def cookies: Seq[Cookie] =
    underlyingHttpServerExchange.requestCookies().asScala.map(CookieUtils.fromUndertow).toSeq

  /*** QUERY ***/
  override lazy val queryParamsRaw: QueryStringMap =
    underlyingHttpServerExchange.getQueryParameters.asScala.toMap.map { (k, v) =>
      (k, v.asScala.toSeq)
    }


  /*** BODY ***/
  private val formBodyParserFactory = locally {
    val parserFactoryBuilder = FormParserFactory.builder
    parserFactoryBuilder.setDefaultCharset("utf-8")
    parserFactoryBuilder.build
  }

  override lazy val bodyString: String =
    String(underlyingHttpServerExchange.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
  
  def bodyFormRaw: FormDataMap =
    // createParser returns null if content-type is not suitable
    val parser = formBodyParserFactory.createParser(underlyingHttpServerExchange)
    Option(parser) match
      case None => throw SharafException("The specified content type is not supported")
      case Some(parser) =>
        val uFormData = parser.parseBlocking()
        UndertowSharafRequest.undertowFormData2FormsonMap(uFormData)
  
}

object UndertowSharafRequest {

  def create(underlyingHttpServerExchange: UHttpServerExchange): UndertowSharafRequest =
    UndertowSharafRequest(underlyingHttpServerExchange)

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
