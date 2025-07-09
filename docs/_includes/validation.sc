//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("cars") =>
    val qp = Request.current.queryParamsValidated[CarQuery]
    Response.withBody(CarApiResult(s"Query OK: ${qp}"))

  case POST -> Path("cars") =>
    val json = Request.current.bodyJsonValidated[Car]
    Response.withBody(CarApiResult(s"JSON body OK: ${json}"))

UndertowSharafServer("localhost", 8181, routes, exceptionMapper = ExceptionMapper.json).start()

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
