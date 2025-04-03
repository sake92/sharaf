//> using scala "3.6.4"
//> using dep ba.sake::sharaf:0.9.2

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
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


object IndexView extends HtmlPage:
  override def pageContent = div(
    p("Welcome!"),
    a(href := "/hello/Bob")("Hello world")
  )

class HelloView(name: String) extends HtmlPage:
  override def pageContent =
    div("Hello ", b(name), "!")
