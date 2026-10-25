// JVM baseline for the native examples; run from this directory:
// scala hello-undertow.scala --server=false
// curl http://localhost:8181/hello/Scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.20.0
//> using file native/ExampleRoutes.scala

import ba.sake.sharaf.undertow.UndertowSharafServer
import nativeexample.ExampleRoutes

@main def run(): Unit =
  UndertowSharafServer("127.0.0.1", 8181, ExampleRoutes.handler).start()
  println("Server started at http://localhost:8181")
