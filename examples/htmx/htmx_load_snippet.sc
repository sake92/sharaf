//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case POST -> Path("html-snippet") =>
    Response.withBody(
      div(
        b("WOW, it works! 😲"),
        div("Look ma, no JS! 😎")
      )
    )

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView = doctype("html")(
  html(
    head(
      script(src := "https://unpkg.com/htmx.org@2.0.4")
    ),
    body(
      button(hx.post := "/html-snippet", hx.swap := "outerHTML")("Click here!")
    )
  )
)
