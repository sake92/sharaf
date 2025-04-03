//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

import io.undertow.Undertow
import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*, routing.*

val routes = Routes:
  case GET -> Path("cars") =>
    val qp = Request.current.queryParamsValidated[CarQuery]
    val filteredCars = qp.model match
      case Some(m) => CarsDb.findByModel(m)
      case None    => CarsDb.findAll()
    Response.withBody(filteredCars)

  case POST -> Path("cars") =>
    val newCar = Request.current.bodyJsonValidated[Car]
    CarsDb.add(newCar)
    Response.withBody(newCar)

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(
    SharafHandler(routes).withExceptionMapper(ExceptionMapper.json)
  )
  .build
  .start()

println("Server started at http://localhost:8181")


object CarsDb {
  var db: Seq[Car] = Seq()
  def findAll(): Seq[Car] = db
  def findByModel(model: String): Seq[Car] = db.filter(_.model == model)
  def add(car: Car): Unit = db = db.appended(car)
}

case class Car(model: String, quantity: Int) derives JsonRW

case class CarQuery(model: Option[String]) derives QueryStringRW
