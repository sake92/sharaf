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
    Response.withBody(CarApiResult(s"Query OK: ${qp}"))

  case POST -> Path("cars") =>
    val json = Request.current.bodyJsonValidated[Car]
    Response.withBody(CarApiResult(s"JSON body OK: ${json}"))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(
    SharafHandler(routes).withExceptionMapper(ExceptionMapper.json)
  )
  .build
  .start()

println(s"Server started at http://localhost:8181")


case class Car(brand: String, model: String, quantity: Int) derives JsonRW
object Car:
  given Validator[Car] = Validator
    .derived[Car]
    .notBlank(_.brand)
    .notBlank(_.model)
    .nonNegative(_.quantity)

case class CarQuery(brand: String) derives QueryStringRW
object CarQuery:
  given Validator[CarQuery] = Validator
    .derived[CarQuery]
    .notBlank(_.brand)

case class CarApiResult(message: String) derives JsonRW