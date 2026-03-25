//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.17.0

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")
  case GET -> _ =>
    Response.withBody("Go to http://localhost:8181/hello/your-name")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
