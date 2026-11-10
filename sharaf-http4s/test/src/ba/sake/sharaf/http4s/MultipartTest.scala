package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractMultipartTest
import scala.concurrent.duration.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory

class MultipartTest extends AbstractMultipartTest {
  private given LoggerFactory[IO] = NoOpFactory[IO]

  override def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"0")
      .withShutdownTimeout(2.seconds)
      .withHttpApp(SharafHttpApp(SharafHandler.exceptions(SharafHandler.routes(routes), mapper)))
      .build
      .use(server => IO.blocking(check(s"http://127.0.0.1:${server.address.port.value}")))
      .unsafeRunSync()
  }
}
