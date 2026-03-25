//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.17.0

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("string", x) =>
    Response.withBody(s"string = ${x}")
  case GET -> Path("int", param[Int](x)) =>
    Response.withBody(s"int = ${x}")
  case GET -> _ =>
    Response.withBody("Try http://localhost:8181/int/123 or http://localhost:8181/string/123abc")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
