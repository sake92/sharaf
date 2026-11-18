//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0
//> using dep ba.sake::sharaf-hepek-components:0.24.0

import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.*
import ba.sake.sharaf.hepek.given
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")


object IndexView extends HtmlPage:
  override def pageContent = div(
    p("Welcome!"),
    a(href := "/hello/Bob")("Hello world")
  )

class HelloView(name: String) extends HtmlPage:
  override def pageContent =
    div("Hello ", b(name), "!")
