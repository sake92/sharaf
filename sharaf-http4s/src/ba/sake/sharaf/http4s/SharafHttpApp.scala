package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import cats.data.Kleisli
import cats.effect.*
import org.http4s.*
import org.typelevel.ci.*

def SharafHttpApp(sharafHandler: SharafHandler) =
  Kleisli[IO, Http4sRequest, Http4sResponse] { (http4sRequest: Http4sRequest) =>
    IO(Http4sSharafRequest(http4sRequest)).bracket { request =>
      for
        path <- IO.pure(RequestDecoding.path(http4sRequest.uri.path.renderString))
        method <- IO.pure(http4sRequest.method match {
          case org.http4s.Method.GET     => HttpMethod.GET
          case org.http4s.Method.POST    => HttpMethod.POST
          case org.http4s.Method.PUT     => HttpMethod.PUT
          case org.http4s.Method.DELETE  => HttpMethod.DELETE
          case org.http4s.Method.OPTIONS => HttpMethod.OPTIONS
          case org.http4s.Method.PATCH   => HttpMethod.PATCH
          case org.http4s.Method.HEAD    => HttpMethod.HEAD
        })
        requestParams <- IO.pure((method = method, path = path))
        response <- IO.blocking(sharafHandler.handle(RequestContext(requestParams, request)))

        headers <- IO.pure {
          val bodyHeaders = response.body.toSeq.flatMap(response.rw.headers)
          val finalHeaders = response.headerUpdates.resolve(bodyHeaders)
          val rawHeaders = finalHeaders.flatMap {
            case HeaderUpdate.Set(name, values) => values.map(value => Header.Raw(CIString(name.value), value))
            case HeaderUpdate.Remove(_)         => Seq.empty
          }
          val cookieHeaders = response.cookieUpdates.updates.map { cookie =>
            Header.Raw(CIString("Set-Cookie"), CookieHeaders.render(cookie))
          }
          Headers(rawHeaders ++ cookieHeaders)
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
              .getOrElse(
                throw exceptions.SharafException(s"${response.status} can't be converted to org.http4s.Status")
              ),
            httpVersion = HttpVersion.`HTTP/1.1`,
            headers = headers,
            entity = Entity.stream(body)
          )
        )
      yield response
    }(request => IO.blocking(request.closeUploads()))
  }
