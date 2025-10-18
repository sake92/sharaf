package ba.sake.sharaf.http4s

import cats.effect.*
import cats.effect.unsafe.implicits.global
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import org.http4s.UrlForm

import scala.collection.immutable.SeqMap

class Http4sSharafRequest(underlyingRequest: Http4sRequest) extends Request {

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

  def bodyFormRaw: FormDataMap =
    val io = for
      urlForm <- underlyingRequest.as[UrlForm]
      builder <- IO(SeqMap.newBuilder[String, Seq[FormValue]])
      _ <- IO {
        urlForm.values.foreach { case (key, values) =>
          key -> values.map { value =>
            FormValue.Str(value)
          }.toList
        }
      }
      result <- IO(builder.result())
    yield result

    io.unsafeRunSync()

  override def toString(): String =
    s"Http4sSharafRequest(headers=${headers}, cookies=${cookies}, queryParamsRaw=${queryParamsRaw}, bodyString=...)"
}

object Http4sSharafRequest {

  def create(underlyingRequest: Http4sRequest): Http4sSharafRequest =
    Http4sSharafRequest(underlyingRequest)
}
