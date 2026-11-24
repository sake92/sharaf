package ba.sake.sharaf.http4s

import java.io.{FilterInputStream, InputStream}
import cats.effect.*
import cats.effect.unsafe.implicits.global
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*

class Http4sSharafRequest(underlyingRequest: Http4sRequest) extends MultipartRequest {

  private var openedBody: Option[InputStream] = None

  private[http4s] def closeResources(): Unit =
    try openedBody.foreach(_.close())
    finally closeUploads()

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
  override lazy val bodyInputStream: InputStream = {
    val (input, release) = fs2.io.toInputStreamResource(underlyingRequest.body).allocated.unsafeRunSync()
    val stream = new FilterInputStream(input) {
      private var closed = false

      override def close(): Unit =
        if !closed then {
          closed = true
          release.unsafeRunSync()
        }
    }
    openedBody = Some(stream)
    stream
  }

  override lazy val bodyString: String =
    underlyingRequest.body.through(fs2.text.utf8.decode).compile.string.unsafeRunSync()

  private[sharaf] override def readMultipartForm(contentType: String): FormDataMap =
    fs2.io
      .toInputStreamResource(underlyingRequest.body)
      .use { input =>
        IO.blocking(parseMultipartForm(input, contentType))
      }
      .unsafeRunSync()

  override def toString(): String =
    "Http4sSharafRequest(...)"
}

object Http4sSharafRequest {

  def create(underlyingRequest: Http4sRequest): Http4sSharafRequest =
    Http4sSharafRequest(underlyingRequest)
}
