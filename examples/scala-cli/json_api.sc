//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

import io.undertow.Undertow
import ba.sake.tupson.JsonRW
import ba.sake.sharaf.*, routing.*

val routes = Routes:  
  case GET -> Path("cars") =>
    Response.withBody(CarsDb.findAll())

  case GET -> Path("cars", brand) =>
    val res = CarsDb.findByBrand(brand)
    Response.withBody(res)

  case POST -> Path("cars") =>
    val reqBody = Request.current.bodyJson[Car]
    CarsDb.add(reqBody)
    Response.withBody(reqBody)

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(
    SharafHandler(routes).withExceptionMapper(ExceptionMapper.json)
  )
  .build
  .start()

println("Server started at http://localhost:8181")

case class Car(brand: String, model: String, quantity: Int) derives JsonRW

object CarsDb {
  var db: Seq[Car] = Seq()
  def findAll(): Seq[Car] = db
  def findByBrand(brand: String): Seq[Car] = db.filter(_.brand == brand)
  def add(car: Car): Unit = db = db.appended(car)
}