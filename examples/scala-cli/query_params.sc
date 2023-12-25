//> using scala "3.3.1"
//> using dep ba.sake::sharaf:0.0.17

import io.undertow.Undertow
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*, routing.*

case class SearchParams(q: String, perPage: Int) derives QueryStringRW

val routes = Routes:
  case GET() -> Path("raw") =>
    val qp = Request.current.queryParamsMap
    Response.withBody(s"params = ${qp}")

  case GET() -> Path("typed") =>
    val qp = Request.current.queryParams[SearchParams]
    Response.withBody(s"params = ${qp}")

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")