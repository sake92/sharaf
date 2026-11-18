//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("raw") =>
    val qp = Request.current.queryParamsRaw
    Response.withBody(s"raw params = ${qp}")

  case GET -> Path("typed") =>
    case class SearchParams(q: String, perPage: Int) derives QueryStringRW
    val qp = Request.current.queryParams[SearchParams]
    Response.withBody(s"typed params = ${qp}")

  case GET -> _ =>
    Response.withBody("Try http://localhost:8181/raw?q=scala&perPage=10 or http://localhost:8181/typed?q=scala&perPage=10")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")
