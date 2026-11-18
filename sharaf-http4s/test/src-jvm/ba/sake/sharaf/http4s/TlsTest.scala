package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory

class TlsTest extends munit.FunSuite {
  private given LoggerFactory[IO] = NoOpFactory[IO]

  test("shared TLS configuration serves HTTPS") {
    val tls = TlsTestSupport.config()
    val routes = Routes { case GET -> Path() => Response.withBody("secure") }
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"0")
      .withTLS(Http4sTls.context[IO](tls))
      .withHttpApp(SharafHttpApp(SharafHandler.routes(routes)))
      .build
      .use(server =>
        IO.blocking {
          val response = TlsTestSupport.get(tls, server.address.port.value)
          assert(response.startsWith("HTTP/1.1 200"))
          assert(response.contains("secure"))
        }
      )
      .unsafeRunSync()
  }
}
