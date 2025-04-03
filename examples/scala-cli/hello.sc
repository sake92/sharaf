//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
