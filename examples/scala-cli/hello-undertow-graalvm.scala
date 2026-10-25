// Build from this directory (requires a C toolchain; Scala CLI downloads GraalVM):
// scala --power package hello-undertow-graalvm.scala --server=false --native-image \
//   --graalvm-jvm-id graalvm-community:25.0.2 -o /tmp/sharaf-graalvm -- --no-fallback -march=compatibility
// /tmp/sharaf-graalvm
// curl http://localhost:8181/hello/Scala
// curl -H 'Content-Type: application/json' -d '{"name":"Scala"}' http://localhost:8181/hello
//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.20.0
//> using resourceDir graalvm-resources

import ba.sake.tupson.JsonRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

case class GreetingRequest(name: String) derives JsonRW
case class Greeting(message: String) derives JsonRW

val routes = Routes:
  case GET -> Path("plaintext") =>
    Response.withBody("Hello Scala")
  case GET -> Path("hello", name) =>
    Response.withBody(Greeting(s"Hello $name"))
  case POST -> Path("hello") =>
    val request = Request.current.bodyJson[GreetingRequest]
    Response.withBody(Greeting(s"Hello ${request.name}"))

@main def run(): Unit =
  UndertowSharafServer("127.0.0.1", 8181, routes, exceptionMapper = ExceptionMapper.json).start()
  println("Server started at http://localhost:8181")
