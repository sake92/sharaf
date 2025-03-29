//> using scala "3.4.2"
//> using dep ba.sake::sharaf:0.9.0

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*, routing.*

object IndexView extends HtmlPage with HtmxDependencies:
  override def pageContent =
    button(hx.post := "/html-snippet", hx.swap := "outerHTML")("Click here!")

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
