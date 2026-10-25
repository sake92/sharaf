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
//> using file native/ExampleRoutes.scala

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory
import ba.sake.sharaf.http4s.*
import nativeexample.ExampleRoutes

given LoggerFactory[IO] = NoOpFactory[IO]

object Main extends IOApp.Simple:
  def run =
    EmberServerBuilder
      .default[cats.effect.IO]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"8181")
      .withHttpApp(SharafHttpApp(ExampleRoutes.handler))
      .build
      .evalTap(server => IO.println(s"Server started at ${server.address}"))
      .useForever
