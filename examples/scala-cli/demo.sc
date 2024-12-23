//> using scala "3.4.2"
//> using dep ba.sake::sharaf:0.8.0

import io.undertow.Undertow
import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*, routing.*

case class Car(model: String, quantity: Int) derives JsonRW

case class CarQuery(model: Option[String]) derives QueryStringRW

var carsDB = Seq[Car]()

val routes = Routes:
  case GET() -> Path("cars") =>
    val qp = Request.current.queryParamsValidated[CarQuery]
    val filteredCars = qp.model match
      case Some(b) => carsDB.filter(_.model == b)
      case None    => carsDB
    Response.withBody(filteredCars)

  case POST() -> Path("cars") =>
    val newCar = Request.current.bodyJsonValidated[Car]
    carsDB = carsDB.appended(newCar)
    Response.withBody(newCar)

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(
    SharafHandler(routes).withExceptionMapper(ExceptionMapper.json)
  )
  .build
  .start()

println(s"Server started at http://localhost:8181")
