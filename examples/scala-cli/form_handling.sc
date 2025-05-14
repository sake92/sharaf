//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import scalatags.Text.all.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(ContactUsView)
  case POST -> Path("handle-form") =>
    val formData = Request.current.bodyForm[ContactUsForm]
    Response.withBody(s"Got form data: ${formData}")

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")


def ContactUsView = doctype("html")(
  html(
    body(
      form(action := "/handle-form", method := "POST")(
        div(
          label("Full Name: ", input(name := "fullName", autofocus))
        ),
        div(
          label("Email: ", input(name := "email", tpe := "email"))
        ),
        input(tpe := "Submit")
      )
    )
  )
)

case class ContactUsForm(fullName: String, email: String) derives FormDataRW
