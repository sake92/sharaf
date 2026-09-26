import ba.sake.sharaf.*
import ba.sake.sharaf.http4s.*

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.*
import _root_.org.typelevel.log4cats.LoggerFactory
import _root_.org.typelevel.log4cats.noop.NoOpFactory

val routes = Routes {
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello ${name}!")
  case _ =>
    Response.withBody("Hello Http4s!")
}

given LoggerFactory[IO] = NoOpFactory[IO]

object Main extends IOApp.Simple:
  def run =
    EmberServerBuilder
      .default[cats.effect.IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(SharafHttpApp(SharafHandler.routes(routes)))
      .build
      .useForever
