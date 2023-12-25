//> using scala "3.3.1"
//> using dep ba.sake::sharaf:0.0.18

import io.undertow.Undertow
import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*, routing.*

case class Car(brand: String, model: String, quantity: Int) derives JsonRW
object Car:
  given Validator[Car] = Validator
    .derived[Car]
    .notBlank(_.brand)
    .notBlank(_.model)
    .nonnegative(_.quantity)

case class CarQuery(brand: String) derives QueryStringRW
object CarQuery:
  given Validator[CarQuery] = Validator
    .derived[CarQuery]
    .notBlank(_.brand)

case class CarApiResult(message: String) derives JsonRW

val routes = Routes:
  case GET() -> Path("cars") =>
    val qp = Request.current.queryParamsValidated[CarQuery]
    Response.withBody(CarApiResult("Query OK"))

  case POST() -> Path("cars") =>
    val qp = Request.current.bodyJsonValidated[Car]
    Response.withBody(CarApiResult("JSON body OK"))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(
    SharafHandler(routes).withErrorMapper(ErrorMapper.json)
  )
  .build
  .start()

println(s"Server started at http://localhost:8181")
