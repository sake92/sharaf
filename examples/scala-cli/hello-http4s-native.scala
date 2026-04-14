// you can build it as native executable with Scala CLI, and run it without JVM
// scala --power package hello-http4s-native.scala -o http4s
// ./http4s
//> using scala 3.7.4
//> using platform native
//> using nativeVersion 0.5.9
//> using dep ba.sake::sharaf-http4s::0.18.0
//> using dep org.http4s::http4s-ember-server::0.23.34

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.*
import ba.sake.sharaf.*
import ba.sake.sharaf.http4s.*

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")
  case GET -> _ =>
    Response.withBody("Go to http://localhost:8181/hello/your-name")

object Main extends IOApp.Simple:
  def run =
    EmberServerBuilder
      .default[cats.effect.IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8181")
      .withHttpApp(SharafHttpApp(SharafHandler.routes(routes)))
      .build
      .evalTap(server => IO.println(s"Server started at ${server.address}"))
      .useForever

