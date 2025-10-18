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

      headers <- IO.pure(Headers(response.headerUpdates.updates.flatMap {
        case HeaderUpdate.Set(name, values) =>
          values.map(value => Header.Raw(CIString(name.value), value))
        case HeaderUpdate.Remove(name) =>
          Seq.empty // TODO: remove header
      }))

      body <- IO.pure(response.body match {
        case Some(body) =>
          fs2.io.readOutputStream(4096)(outputStream => IO.blocking(response.rw.write(body, outputStream)))
        case None =>
          fs2.Stream.empty[IO]
      })

      response <- IO.pure(
        Http4sResponse[IO](
          status = Status
            .fromInt(response.status.code)
            .getOrElse(throw exceptions.SharafException(s"${response.status} can't be converted to org.http4s.Status")),
          httpVersion = HttpVersion.`HTTP/1.1`,
          headers = headers,
          body = body
        )
      )
    yield response
  }
