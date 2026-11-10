package ba.sake.sharaf.helidon

import java.nio.charset.StandardCharsets
import scala.jdk.StreamConverters.*
import scala.jdk.CollectionConverters.*
import io.helidon.webserver.http.ServerRequest
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.UnsupportedMediaTypeException

class HelidonSharafRequest(underlyingRequest: ServerRequest) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers()
    underlyingHeaders.stream
      .toScala(LazyList)
      .map { header =>
        HttpString(header.name()) -> header.allValues().asScala.toSeq
      }
      .toMap

  def cookies: Seq[Cookie] =
    val values = underlyingRequest.headers().all(io.helidon.http.HeaderNames.COOKIE, () => java.util.List.of[String]())
    CookieHeaders.parse(values.asScala.toSeq)

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(underlyingRequest.query().rawValue())

  /* *** BODY *** */
  override lazy val bodyString: String =
    String(underlyingRequest.content().inputStream().readAllBytes(), StandardCharsets.UTF_8)

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    val mediaType = contentType.takeWhile(_ != ';').trim
    if mediaType.equalsIgnoreCase("application/x-www-form-urlencoded") then UrlEncoded.form(bodyString)
    else if Multipart.isMultipart(contentType) then
      Multipart.parse(underlyingRequest.content().inputStream(), contentType, uploads)
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")

}

object HelidonSharafRequest {

  def create(underlyingRequest: ServerRequest): HelidonSharafRequest =
    HelidonSharafRequest(underlyingRequest)
}
