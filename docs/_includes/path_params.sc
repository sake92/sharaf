//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("string", x) =>
    Response.withBody(s"string = ${x}")

  case GET -> Path("int", param[Int](x)) =>
    Response.withBody(s"int = ${x}")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
