//> using scala 3.7.0
//> using dep ba.sake::sharaf-undertow:0.17.0

import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case POST -> Path("html-snippet") =>
    Response.withBody:
      html"""
        <div>
        <b>WOW, it works! 😲</b>
        <div>Look ma, no JS! 😎</div>
        </div>
      """

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView =
  html"""
    <!DOCTYPE html>
    <html>
    <head>
      <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    </head>
    <body>
      <button hx-post="/html-snippet" hx-swap="outerHTML">Click here!</button>
    </body>
    </html>
  """
