//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(ContactUsView)
  case POST -> Path("handle-form") =>
    val formData = Request.current.bodyForm[ContactUsForm]
    Response.withBody(s"Got form data: ${formData}")

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

def ContactUsView =
  html"""
    <!DOCTYPE html>
    <html>
    <body>
        <form action="/handle-form" method="POST">
            <div>
                <label>Full Name: <input type="text" name="fullName" autofocus></label>
            </div>
            <div>
                <label>Email: <input type="email" name="email"></label>
            </div>
            <input type="submit" value="Submit">
        </form>
    </body>
    </html>
  """

case class ContactUsForm(fullName: String, email: String) derives FormDataRW
