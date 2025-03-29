//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.0

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.sharaf.*, routing.*

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(
      doctype("html")(
        html(
          p("Welcome!"),
          a(href := "/hello/Bob")("Go to /hello/Bob")
        )
      )
    )
  case GET -> Path("hello", name) =>
    Response.withBody(
      doctype("html")(
        html(
          p("Welcome!"),
          div("Hello ", b(name), "!")
        )
      )
    )

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
