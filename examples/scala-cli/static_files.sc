//> using scala "3.4.2"
//> using dep ba.sake::sharaf:0.8.0

import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*

val routes = Routes:
  case GET() -> Path() =>
    Response.withBody("Try /example.js")

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
