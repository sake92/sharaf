//> using scala "3.3.1"
//> using dep ba.sake::sharaf:0.0.17

import io.undertow.Undertow
import ba.sake.tupson.JsonRW
import ba.sake.sharaf.*, routing.*

case class Car(brand: String, model: String, quantity: Int) derives JsonRW

var db: Seq[Car] = Seq()

val routes = Routes {
  case GET() -> Path("cars") =>
    Response.withBody(db)
    
  case GET() -> Path("cars", brand) =>
    val res = db.filter(_.brand == brand)
    Response.withBody(res)

  case POST() -> Path("cars") =>
    val qp = Request.current.bodyJson[Car]
    db = db.appended(qp)
    Response.withBody(db)
}

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
