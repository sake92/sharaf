package ba.sake.sharaf.http4s

import cats.effect.*
import cats.effect.unsafe.implicits.global
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import org.http4s.UrlForm

import scala.collection.immutable.SeqMap

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
    if Multipart.isMultipart(contentType) then
      fs2.io
        .toInputStreamResource(underlyingRequest.body)
        .use { input =>
          IO.blocking(Multipart.parse(input, contentType, uploads))
        }
        .unsafeRunSync()
    else urlEncodedForm

  private def urlEncodedForm: FormDataMap =
    val urlForm = underlyingRequest.as[UrlForm].unsafeRunSync()
    SeqMap.from(urlForm.values.iterator.map { case (key, values) =>
      key -> values.map(FormValue.Str.apply).toList
    })

  override def toString(): String =
    s"Http4sSharafRequest(headers=${headers}, cookies=${cookies}, queryParamsRaw=${queryParamsRaw}, bodyString=...)"
}

object Http4sSharafRequest {

  def create(underlyingRequest: Http4sRequest): Http4sSharafRequest =
    Http4sSharafRequest(underlyingRequest)
}
