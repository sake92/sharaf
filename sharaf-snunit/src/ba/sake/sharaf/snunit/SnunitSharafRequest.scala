package ba.sake.sharaf.snunit

import java.nio.charset.StandardCharsets
import snunit.{Request as SnunitRequest, *}
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*

class SnunitSharafRequest(underlyingRequest: SnunitRequest) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers
    underlyingHeaders.toMap
      .map { (headerName, headerValue) =>
        HttpString(headerName) -> Seq(headerValue)
      }

  override def cookies: Seq[Cookie] =
    val cookieHeaders = Seq.newBuilder[String]
    underlyingRequest.headers.foreach { (name, value) =>
      if name.equalsIgnoreCase("Cookie") then cookieHeaders += value
    }
    CookieHeaders.parse(cookieHeaders.result())

  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(underlyingRequest.query)

  override lazy val bodyString: String =
    String(underlyingRequest.contentRaw(), StandardCharsets.UTF_8)

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    if Multipart.isMultipart(contentType) then
      Multipart.parse(new java.io.ByteArrayInputStream(underlyingRequest.contentRaw()), contentType, uploads)
    else super.bodyFormRaw

}

object SnunitSharafRequest {

  def create(underlyingRequest: SnunitRequest): SnunitSharafRequest =
    SnunitSharafRequest(underlyingRequest)
}
