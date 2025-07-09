//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

import scalatags.Text.all.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

UndertowSharafServer("localhost", 8181, routes).start()

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
