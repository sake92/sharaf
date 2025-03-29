//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.0

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.formson.FormDataRW
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.*, routing.*

object ContacUsView extends HtmlPage:
  override def pageContent =
    form(action := "/handle-form", method := "POST")(
      div(
        label("Full Name: ", input(name := "fullName", autofocus))
      ),
      div(
        label("Email: ", input(name := "email", tpe := "email"))
      ),
      input(tpe := "Submit")
    )

case class ContactUsForm(fullName: String, email: String) derives FormDataRW

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(ContacUsView)

  case POST -> Path("handle-form") =>
    val formData = Request.current.bodyForm[ContactUsForm]
    Response.withBody(s"Got form data: ${formData}")

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
