package ba.sake.sharaf.undertow

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import io.undertow.server.HttpServerExchange
import io.undertow.UndertowOptions
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*

final class UndertowSharafRequest(val underlyingHttpServerExchange: HttpServerExchange) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  def headers: Map[HttpString, Seq[String]] =
    val hMap = underlyingHttpServerExchange.getRequestHeaders
    hMap.getHeaderNames.asScala.map { name =>
      HttpString(name.toString) -> hMap.get(name).asScala.toSeq
    }.toMap

  override def cookies: Seq[Cookie] =
    val limit = underlyingHttpServerExchange.getConnection.getUndertowOptions
      .get(UndertowOptions.MAX_COOKIES, UndertowOptions.DEFAULT_MAX_COOKIES)
    CookieHeaders.parse(headers.getOrElse(HttpString("Cookie"), Seq.empty), limit)

  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(underlyingHttpServerExchange.getQueryString)

  override lazy val bodyString: String = {
    val input = underlyingHttpServerExchange.getInputStream
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    if UrlEncoded.isForm(contentType) then
      try UrlEncoded.readFormBody(input)
      catch {
        case error: PayloadTooLargeException =>
          underlyingHttpServerExchange.setPersistent(false)
          throw error
      }
    else String(input.readAllBytes(), StandardCharsets.UTF_8)
  }

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    if Multipart.isMultipart(contentType) then
      Multipart.parse(underlyingHttpServerExchange.getInputStream, contentType, uploads)
    else super.bodyFormRaw

  override def toString(): String =
    "UndertowSharafRequest(...)"
}

object UndertowSharafRequest {

  def create(underlyingHttpServerExchange: HttpServerExchange): UndertowSharafRequest =
    UndertowSharafRequest(underlyingHttpServerExchange)

}
