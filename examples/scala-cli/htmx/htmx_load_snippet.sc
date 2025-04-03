//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

// scala htmx_load_snippet.sc --resource-dir resources

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*, routing.*

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

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

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
