package fullstack

import io.undertow.Undertow
import ba.sake.validson.*
import ba.sake.sharaf.*, routing.*
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
          Response.withBody(ShowFormPage(formData, errors)).withStatus(400)

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()
}
