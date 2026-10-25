// you can build it as native executable with Scala CLI, and run it without JVM
// scala --power package hello-http4s-native.scala --server=false --native-mode release-fast \
//   --native-multithreading -o /tmp/sharaf-native
// /tmp/sharaf-native
// curl http://localhost:8181/hello/Scala
//> using scala 3.9.0
//> using platform native
//> using nativeVersion 0.5.12
//> using dep ba.sake::sharaf-http4s::0.20.0
//> using dep org.http4s::http4s-ember-server::1.0.0-M48

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory
import ba.sake.sharaf.http4s.*
import ba.sake.sharaf.*
import ba.sake.tupson.JsonRW

case class GreetingRequest(name: String) derives JsonRW
case class Greeting(message: String) derives JsonRW

val routes = Routes:
  case GET -> Path("plaintext") =>
    Response.withBody("Hello Scala").settingHeader("Content-Type", "text/plain; charset=utf-8")
  case GET -> Path("hello", name) =>
    Response.withBody(Greeting(s"Hello $name")).settingHeader("Content-Type", "application/json; charset=utf-8")
  case POST -> Path("hello") =>
    val request = Request.current.bodyJson[GreetingRequest]
    Response
      .withBody(Greeting(s"Hello ${request.name}"))
      .settingHeader("Content-Type", "application/json; charset=utf-8")

// Published Sharaf 0.20.0's http4s adapter needs explicit body headers.
val jsonErrors: ExceptionMapper =
  ExceptionMapper.json.andThen(_.settingHeader("Content-Type", "application/json; charset=utf-8"))

given LoggerFactory[IO] = NoOpFactory[IO]

object Main extends IOApp.Simple:
  def run =
    EmberServerBuilder
      .default[cats.effect.IO]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"8181")
      .withHttpApp(SharafHttpApp(SharafHandler.exceptions(SharafHandler.routes(routes), jsonErrors)))
      .build
      .evalTap(server => IO.println(s"Server started at ${server.address}"))
      .useForever
