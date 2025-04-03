//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.sharaf.*, routing.*

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")

def IndexView = doctype("html")(
  html(
    p("Welcome!"),
    a(href := "/hello/Bob")("Go to /hello/Bob")
  )
)

def HelloView(name: String) = doctype("html")(
  html(
    p("Welcome!"),
    div("Hello ", b(name), "!")
  )
)
