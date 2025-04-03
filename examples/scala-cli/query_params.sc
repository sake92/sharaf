//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

import io.undertow.Undertow
import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*, routing.*


val routes = Routes:
  case GET -> Path("raw") =>
    val qp = Request.current.queryParamsRaw
    Response.withBody(s"params = ${qp}")

  case GET -> Path("typed") =>
    case class SearchParams(q: String, perPage: Int) derives QueryStringRW
    val qp = Request.current.queryParams[SearchParams]
    Response.withBody(s"params = ${qp}")

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
