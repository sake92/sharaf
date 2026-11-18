//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody("Try http://localhost:8181/example.js")

PotatehSharafServer(routes, host = "localhost", port = 8181, notFoundHandler = SharafHandler.classpathResources("public")).start()

println(s"Server started at http://localhost:8181")
