// Build from this directory (requires a C toolchain; Scala CLI downloads GraalVM):
// scala --power package hello-jdk-graalvm.scala --server=false --native-image \
//   --graalvm-jvm-id graalvm-community:25.0.2 -o /tmp/sharaf-graalvm -- --no-fallback
// /tmp/sharaf-graalvm
// curl http://localhost:8181/hello/Scala
// curl -H 'Content-Type: application/json' -d '{"name":"Scala"}' http://localhost:8181/hello
//> using scala 3.9.0
//> using dep ba.sake::sharaf-jdk-httpserver:0.20.0
//> using file native/ExampleRoutes.scala

import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import nativeexample.ExampleRoutes

@main def run(): Unit =
  JdkHttpServerSharafServer("127.0.0.1", 8181, ExampleRoutes.handler).start()
  println("Server started at http://localhost:8181")
