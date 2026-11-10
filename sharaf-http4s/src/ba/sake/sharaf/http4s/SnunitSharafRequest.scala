package ba.sake.sharaf.http4s

import cats.effect.*
import cats.effect.unsafe.implicits.global
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*

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

  override def cookies: Seq[Cookie] =
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
    if Multipart.isMultipart(contentType) then
      fs2.io
        .toInputStreamResource(underlyingRequest.body)
        .use { input =>
          IO.blocking(Multipart.parse(input, contentType, uploads))
        }
        .unsafeRunSync()
    else super.bodyFormRaw

  override def toString(): String =
    "Http4sSharafRequest(...)"
}

object Http4sSharafRequest {

  def create(underlyingRequest: Http4sRequest): Http4sSharafRequest =
    Http4sSharafRequest(underlyingRequest)
}
