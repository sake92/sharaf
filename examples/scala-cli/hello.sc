//> using dep ba.sake::sharaf:0.0.11

import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*

val routes: Routes =
  case GET() -> Path("hello", name) =>
    Response.withBody(s"Hello $name")

val server = Undertow
  .builder()
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build()

server.start()

println(s"Server started at http://localhost:8181")
