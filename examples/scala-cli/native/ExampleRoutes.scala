package nativeexample

import ba.sake.tupson.JsonRW
import ba.sake.sharaf.*

case class GreetingRequest(name: String) derives JsonRW
case class Greeting(message: String) derives JsonRW

object ExampleRoutes:
  val routes: Routes = Routes:
    case GET -> Path("plaintext") =>
      Response.withBody("Hello Scala").settingHeader("Content-Type", "text/plain; charset=utf-8")
    case GET -> Path("hello", name) =>
      Response.withBody(Greeting(s"Hello $name")).settingHeader("Content-Type", "application/json; charset=utf-8")
    case POST -> Path("hello") =>
      val request = Request.current.bodyJson[GreetingRequest]
      Response
        .withBody(Greeting(s"Hello ${request.name}"))
        .settingHeader("Content-Type", "application/json; charset=utf-8")

  // Published Sharaf 0.20.0's http4s adapter needs explicit body headers.
  private val jsonErrors: ExceptionMapper =
    ExceptionMapper.json.andThen(_.settingHeader("Content-Type", "application/json; charset=utf-8"))

  val handler: SharafHandler = SharafHandler.exceptions(SharafHandler.routes(routes), jsonErrors)
