package ba.sake.sharaf.jdkhttp

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import com.sun.net.httpserver.HttpExchange
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.UnsupportedMediaTypeException

final class JdkHttpServerSharafRequest(val underlyingHttpExchange: HttpExchange) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    underlyingHttpExchange.getRequestHeaders.asScala.toMap.map { (name, values) =>
      HttpString(name) -> values.asScala.toSeq
    }

  def cookies: Seq[Cookie] =
    headers
      .get(HttpString("Cookie"))
      .toSeq
      .flatten
      .flatMap { cookieHeader =>
        cookieHeader
          .split(";")
          .map(_.trim)
          .map { cookieStr =>
            val parts = cookieStr.split("=", 2)
            if parts.length == 2 then Cookie(name = parts(0).trim, value = parts(1).trim)
            else Cookie(name = parts(0).trim, value = "")
          }
      }

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(Option(underlyingHttpExchange.getRequestURI.getRawQuery).getOrElse(""))

  /* *** BODY *** */
  override lazy val bodyString: String =
    val bytes = underlyingHttpExchange.getRequestBody.readAllBytes()
    String(bytes, StandardCharsets.UTF_8)

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    val mediaType = contentType.takeWhile(_ != ';').trim
    if mediaType.equalsIgnoreCase("application/x-www-form-urlencoded") then UrlEncoded.form(bodyString)
    else if Multipart.isMultipart(contentType) then
      Multipart.parse(underlyingHttpExchange.getRequestBody, contentType, uploads)
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")

  override def toString(): String =
    s"JdkHttpServerSharafRequest(headers=${headers}, cookies=${cookies}, queryParamsRaw=${queryParamsRaw}, bodyString=...)"
}

object JdkHttpServerSharafRequest {
  def create(httpExchange: HttpExchange): JdkHttpServerSharafRequest =
    JdkHttpServerSharafRequest(httpExchange)
}
