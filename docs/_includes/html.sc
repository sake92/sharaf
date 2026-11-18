//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")

def IndexView =
  html"""
    <!DOCTYPE html>
    <html lang="en">
    <body>
    <div>
        <p>Welcome!</p>
        <a href="/hello/Bob">Hello world</a>
    </div>
    </body>
    </html>
  """

def HelloView(name: String) =
  html"""
    <!DOCTYPE html>
    <html lang="en">
    <body>
    <div>
        Hello <b>${name}</b>!
    </div>
    </body>
    </html>
  """
