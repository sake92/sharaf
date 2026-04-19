package fullstack

import sttp.model.StatusCode
import ba.sake.validson.*
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer
import fullstack.views.*

@main def main: Unit =
  val module = FullstackModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class FullstackModule(port: Int) {

  val baseUrl = s"http://localhost:${port}"

  private val routes = Routes:
    case GET -> Path() =>
      Response.withBody(ShowFormPage(CreateCustomerForm.empty))

    case POST -> Path("form-submit") =>
      // note that here we do the validation *manually* !!
      val formData = Request.current.bodyForm[CreateCustomerForm]
      formData.validate match
        case Seq() =>
          Response.withBody(SucessPage(formData))
        case errors =>
          Response.withBody(ShowFormPage(formData, errors)).withStatus(StatusCode.Ok)
    
    case GET -> Path("sse-events") =>
      val sseSender = SseSender()
        .onComplete(() => println("SSE stream completed"))
        .onError(e => println(s"SSE error (client disconnected?): ${e.getMessage}"))
      new Thread(() => {
        for i <- 1 to 5 do
          sseSender.send(
            ServerSentEvent.Message(
              data = html"""<div class="fade-me-in">event${i}</div>""".toString
            )
          )
          Thread.sleep(1_000)
        sseSender.send(ServerSentEvent.Done())
      }).start()
      Response.withBody(sseSender)

  val server = UndertowSharafServer("localhost", port, routes)
}
