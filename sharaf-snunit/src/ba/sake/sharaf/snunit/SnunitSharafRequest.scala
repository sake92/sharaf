package ba.sake.sharaf.snunit

import java.nio.charset.StandardCharsets
import snunit.{Request as SnunitRequest, *}
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.UnsupportedMediaTypeException

class SnunitSharafRequest(underlyingRequest: SnunitRequest) extends Request {

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers
    underlyingHeaders.toMap
      .map { (headerName, headerValue) =>
        HttpString(headerName) -> Seq(headerValue)
      }

  def cookies: Seq[Cookie] =
    val builder = Seq.newBuilder[Cookie]
    val underlyingHeaders = underlyingRequest.headers
    // TODO: Use underlyingRequest.cookieFieldIndex when available
    underlyingHeaders.foreach {
      case (name, cookieString) if name.equalsIgnoreCase("Cookie") =>
        cookieString.split(';').foreach {
          case item if item.trim.nonEmpty =>
            val parts = item.trim.split("=", 2)
            builder += Cookie(name = parts(0).trim, value = if parts.length == 2 then parts(1).trim else "")
          case _ => ()
        }
      case _ =>
    }
    builder.result()

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
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded")

}

object SnunitSharafRequest {

  def create(underlyingRequest: SnunitRequest): SnunitSharafRequest =
    SnunitSharafRequest(underlyingRequest)
}
