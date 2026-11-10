package ba.sake.sharaf.potateh

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.Path
import ba.sake.sharaf.exceptions.{BadRequestException, UnsupportedMediaTypeException}

final class PotatehSharafRequest(val underlying: ba.sake.potateh.Request) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  private[potateh] lazy val uri: URI =
    try {
      val target = underlying.target
      val absolute = if target.startsWith("/") then "http://request.invalid" + target else target
      URI.create(absolute)
    } catch case _: IllegalArgumentException => throw BadRequestException("Invalid request target")

  private[potateh] def path: Path =
    RequestDecoding.path(Option(uri.getRawPath).getOrElse(""))

  override lazy val headers: Map[HttpString, Seq[String]] =
    underlying.headers.fields.groupMap { case (name, _) => HttpString(name) } { case (_, value) => value }

  override lazy val cookies: Seq[Cookie] =
    CookieHeaders.parse(underlying.headers.all("Cookie"))

  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(Option(uri.getRawQuery).getOrElse(""))

  private lazy val bodyBytes: Array[Byte] = underlying.body.readAllBytes()

  override lazy val bodyString: String = new String(bodyBytes, UTF_8)

  override lazy val bodyFormRaw: FormDataMap = {
    val contentType = underlying.headers.first("Content-Type").getOrElse("")
    val mediaType = contentType.takeWhile(_ != ';').trim
    if mediaType.equalsIgnoreCase("application/x-www-form-urlencoded") then UrlEncoded.form(bodyString)
    else if Multipart.isMultipart(contentType) then Multipart.parse(underlying.body, contentType, uploads)
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")
  }

}
