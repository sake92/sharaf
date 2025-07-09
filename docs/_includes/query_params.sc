//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("raw") =>
    val qp = Request.current.queryParamsRaw
    Response.withBody(s"params = ${qp}")

  case GET -> Path("typed") =>
    case class SearchParams(q: String, perPage: Int) derives QueryStringRW
    val qp = Request.current.queryParams[SearchParams]
    Response.withBody(s"params = ${qp}")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
