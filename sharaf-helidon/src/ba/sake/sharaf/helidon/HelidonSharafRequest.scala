package ba.sake.sharaf.helidon

import java.nio.charset.StandardCharsets
import scala.jdk.StreamConverters.*
import io.helidon.webserver.http.ServerRequest
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.UnsupportedMediaTypeException

class HelidonSharafRequest(underlyingRequest: ServerRequest) extends Request {

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    val underlyingHeaders = underlyingRequest.headers()
    underlyingHeaders.stream
      .toScala(LazyList)
      .map { header =>
        HttpString(header.name()) -> header.values().split(",").toSeq
      }
      .toMap

  def cookies: Seq[Cookie] =
    headers.get(HttpString("Cookie")).toSeq.flatten.flatMap { header =>
      header.split(";").toSeq.map(_.trim).filter(_.nonEmpty).map { item =>
        val parts = item.split("=", 2)
        Cookie(parts(0).trim, if parts.length == 2 then parts(1).trim else "")
      }
    }

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
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded")

}

object HelidonSharafRequest {

  def create(underlyingRequest: ServerRequest): HelidonSharafRequest =
    HelidonSharafRequest(underlyingRequest)
}
