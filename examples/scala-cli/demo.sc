//> using scala 3.7.0
//> using dep ba.sake::sharaf-undertow:0.14.0

import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("cars") =>
    val qp = Request.current.queryParams[(model: Option[String])]
    val filteredCars = qp.model match
      case Some(m) => CarsDb.findByModel(m)
      case None    => CarsDb.findAll()
    Response.withBody(filteredCars)

  case POST -> Path("cars") =>
    val newCar = Request.current.bodyJson[Car]
    CarsDb.add(newCar)
    Response.withBody(newCar)

UndertowSharafServer("localhost", 8181, routes, exceptionMapper = ExceptionMapper.json).start()
println("Server started at http://localhost:8181")

object CarsDb {
  var db: Seq[Car] = Seq()
  def findAll(): Seq[Car] = db
  def findByModel(model: String): Seq[Car] = db.filter(_.model == model)
  def add(car: Car): Unit = db = db.appended(car)
}

case class Car(model: String, quantity: Int) derives JsonRW

