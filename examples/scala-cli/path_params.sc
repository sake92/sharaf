//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("string", x) =>
    Response.withBody(s"string = ${x}")
  case GET -> Path("int", param[Int](x)) =>
    Response.withBody(s"int = ${x}")
  case GET -> _ =>
    Response.withBody("Try http://localhost:8181/int/123 or http://localhost:8181/string/123abc")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")
