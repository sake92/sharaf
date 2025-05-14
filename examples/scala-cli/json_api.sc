//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import ba.sake.tupson.JsonRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

case class Car(brand: String, model: String, quantity: Int) derives JsonRW

object CarsDb {
  var db: Seq[Car] = Seq()
  def findAll(): Seq[Car] = db
  def findByBrand(brand: String): Seq[Car] = db.filter(_.brand == brand)
  def add(car: Car): Unit = db = db.appended(car)
}

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

UndertowSharafServer("localhost", 8181, routes)
  .withExceptionMapper(ExceptionMapper.json)
  .start()

println("Server started at http://localhost:8181")
