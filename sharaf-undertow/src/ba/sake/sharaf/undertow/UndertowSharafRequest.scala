package ba.sake.sharaf.undertow

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpServerExchange
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*

final class UndertowSharafRequest(val underlyingHttpServerExchange: HttpServerExchange) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val hMap = underlyingHttpServerExchange.getRequestHeaders
    hMap.getHeaderNames.asScala.map { name =>
      HttpString(name.toString) -> hMap.get(name).asScala.toSeq
    }.toMap

  def cookies: Seq[Cookie] =
    underlyingHttpServerExchange.requestCookies().asScala.map(CookieUtils.fromUndertow).toSeq

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(underlyingHttpServerExchange.getQueryString)

  /* *** BODY *** */

  override lazy val bodyString: String =
    String(underlyingHttpServerExchange.getInputStream.readAllBytes(), StandardCharsets.UTF_8)

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    val mediaType = contentType.takeWhile(_ != ';').trim
    if mediaType.equalsIgnoreCase("application/x-www-form-urlencoded") then UrlEncoded.form(bodyString)
    else if Multipart.isMultipart(contentType) then
      Multipart.parse(underlyingHttpServerExchange.getInputStream, contentType, uploads)
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")

  override def toString(): String =
    "UndertowSharafRequest(...)"
}

object UndertowSharafRequest {

  def create(underlyingHttpServerExchange: HttpServerExchange): UndertowSharafRequest =
    UndertowSharafRequest(underlyingHttpServerExchange)

}
