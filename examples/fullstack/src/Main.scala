package fullstack

import io.undertow.Undertow
import ba.sake.hepek.html.HtmlPage
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
    case GET() -> Path() =>
      Response.withBody(ShowFormPage(): HtmlPage)

    case POST() -> Path("form-submit") =>
      val formData = Request.current.bodyForm[CreateCustomerForm]
      formData.validate match
        case Seq()  => Response.withBody(SucessPage(formData): HtmlPage)
        case errors => Response.withBody(ShowFormPage(Some(formData), errors): HtmlPage).withStatus(400)

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()
}
