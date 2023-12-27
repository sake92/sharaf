//> using scala "3.3.1"
//> using dep ba.sake::sharaf:0.0.18

import java.util.UUID
import io.undertow.Undertow
import ba.sake.sharaf.*, routing.*

val routes = Routes:
  case GET() -> Path("str", p) =>
    Response.withBody(s"str = ${p}")

  case GET() -> Path("int", param[Int](p)) =>
    Response.withBody(s"int = ${p}")

  case GET() -> Path("uuid", param[UUID](p)) =>
    Response.withBody(s"uuid = ${p}")

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
