package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import cats.data.Kleisli
import cats.effect.*
import org.http4s.*
import org.typelevel.ci.*

def SharafHttpApp(sharafHandler: SharafHandler) =
  Kleisli[IO, Http4sRequest, Http4sResponse] { (http4sRequest: Http4sRequest) =>
    for
      request <- IO.pure(Http4sSharafRequest(http4sRequest))
      path <- IO.pure(Path(http4sRequest.uri.path.segments.map(_.encoded)*))
      method <- IO.pure(http4sRequest.method match {
        case org.http4s.Method.GET     => HttpMethod.GET
        case org.http4s.Method.POST    => HttpMethod.POST
        case org.http4s.Method.PUT     => HttpMethod.PUT
        case org.http4s.Method.DELETE  => HttpMethod.DELETE
        case org.http4s.Method.OPTIONS => HttpMethod.OPTIONS
        case org.http4s.Method.PATCH   => HttpMethod.PATCH
        case org.http4s.Method.HEAD    => HttpMethod.HEAD
      })
      requestParams <- IO.pure((method, path))
      response <- IO.blocking(sharafHandler.handle(RequestContext(requestParams, request)))

      headers <- IO.pure {
        val bodyHeaders = response.body.toSeq.flatMap(response.rw.headers).toMap
        val finalHeaders = response.headerUpdates.updates.foldLeft(bodyHeaders) {
          case (headers, HeaderUpdate.Set(name, values)) => headers.updated(name, values)
          case (headers, HeaderUpdate.Remove(name))      => headers.removed(name)
        }
        val rawHeaders = finalHeaders.toSeq.flatMap { case (name, values) =>
          values.map(value => Header.Raw(CIString(name.value), value))
        }
        Headers(rawHeaders)
      }

      body <- response.body match {
        case Some(body) =>
          IO.blocking {
            val baos = new java.io.ByteArrayOutputStream()
            response.rw.write(body, baos)
            fs2.Stream.emits[IO, Byte](baos.toByteArray())
          }
        case None =>
          IO.pure(fs2.Stream.empty[IO])
      }

      response <- IO.pure(
        Http4sResponse[IO](
          status = Status
            .fromInt(response.status.code)
            .getOrElse(throw exceptions.SharafException(s"${response.status} can't be converted to org.http4s.Status")),
          httpVersion = HttpVersion.`HTTP/1.1`,
          headers = headers,
          entity = Entity.stream(body)
        )
      )
    yield response
  }
