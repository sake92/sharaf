//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

UndertowSharafServer("localhost", 8181, routes).start()

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
