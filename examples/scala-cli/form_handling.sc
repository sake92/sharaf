//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(ContactUsView)
  case POST -> Path("handle-form") =>
    case class ContactUsForm(fullName: String, email: String) derives FormDataRW
    val formData = Request.current.bodyForm[ContactUsForm]
    Response.withBody(s"Got form data: ${formData}")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

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
