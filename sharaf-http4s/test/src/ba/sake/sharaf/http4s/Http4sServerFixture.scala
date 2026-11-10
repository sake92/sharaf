package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.ServerFixture
import scala.concurrent.duration.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory

trait Http4sServerFixture extends ServerFixture {
  private given LoggerFactory[IO] = NoOpFactory[IO]

  override def withHandler(handler: SharafHandler)(check: String => Unit): Unit = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"0")
      .withShutdownTimeout(100.millis)
      .withHttpApp(SharafHttpApp(handler))
      .build
      .use(server => IO.blocking(check(s"http://127.0.0.1:${server.address.port.value}")))
      .unsafeRunSync()
  }
}
