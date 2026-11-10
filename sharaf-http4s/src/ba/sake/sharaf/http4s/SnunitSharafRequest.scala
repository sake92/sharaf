package ba.sake.sharaf.http4s

import cats.effect.*
import cats.effect.unsafe.implicits.global
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.UnsupportedMediaTypeException

class Http4sSharafRequest(underlyingRequest: Http4sRequest) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  /* *** HEADERS *** */
  def headers: Map[HttpString, Seq[String]] =
    underlyingRequest.headers.headers
      .groupBy(_.name)
      .map { (name, headers) =>
        HttpString(name.toString) -> headers.map(_.value)
      }

  def cookies: Seq[Cookie] =
    underlyingRequest.cookies.map { cookie =>
      Cookie(name = cookie.name, value = cookie.content)
    }

  /* *** QUERY *** */
  override lazy val queryParamsRaw: QueryStringMap =
    underlyingRequest.uri.query.multiParams

  /* *** BODY *** */
  override lazy val bodyString: String =
    underlyingRequest.body.through(fs2.text.utf8.decode).compile.string.unsafeRunSync()

  override lazy val bodyFormRaw: FormDataMap =
    val contentType = headers.get(HttpString("Content-Type")).flatMap(_.headOption).getOrElse("")
    val mediaType = contentType.takeWhile(_ != ';').trim
    if mediaType.equalsIgnoreCase("application/x-www-form-urlencoded") then UrlEncoded.form(bodyString)
    else if Multipart.isMultipart(contentType) then
      fs2.io
        .toInputStreamResource(underlyingRequest.body)
        .use { input =>
          IO.blocking(Multipart.parse(input, contentType, uploads))
        }
        .unsafeRunSync()
    else throw UnsupportedMediaTypeException("Expected application/x-www-form-urlencoded or multipart/form-data")

  override def toString(): String =
    s"Http4sSharafRequest(headers=${headers}, cookies=${cookies}, queryParamsRaw=${queryParamsRaw}, bodyString=...)"
}

object Http4sSharafRequest {

  def create(underlyingRequest: Http4sRequest): Http4sSharafRequest =
    Http4sSharafRequest(underlyingRequest)
}
