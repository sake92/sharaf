package ba.sake.sharaf.snunit

import java.nio.charset.StandardCharsets
import snunit.{Request as SnunitRequest, *}
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.UnsupportedMediaTypeException

class SnunitSharafRequest(underlyingRequest: SnunitRequest) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers
    underlyingHeaders.toMap
      .map { (headerName, headerValue) =>
        HttpString(headerName) -> Seq(headerValue)
      }

  def cookies: Seq[Cookie] =
    val cookieHeaders = Seq.newBuilder[String]
    underlyingRequest.headers.foreach { (name, value) =>
      if name.equalsIgnoreCase("Cookie") then cookieHeaders += value
    }
    CookieHeaders.parse(cookieHeaders.result())

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(underlyingRequest.query)

  /* *** BODY *** */
  override lazy val bodyString: String =
    String(underlyingRequest.contentRaw(), StandardCharsets.UTF_8)

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    val mediaType = contentType.takeWhile(_ != ';').trim
    if mediaType.equalsIgnoreCase("application/x-www-form-urlencoded") then UrlEncoded.form(bodyString)
    else if Multipart.isMultipart(contentType) then
      Multipart.parse(new java.io.ByteArrayInputStream(underlyingRequest.contentRaw()), contentType, uploads)
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")

}

object SnunitSharafRequest {

  def create(underlyingRequest: SnunitRequest): SnunitSharafRequest =
    SnunitSharafRequest(underlyingRequest)
}
