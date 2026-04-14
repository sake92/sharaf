//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.18.0
//> using dep ba.sake::sharaf-hepek-components:0.17.0

import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.*
import ba.sake.sharaf.hepek.given
import ba.sake.sharaf.undertow.*

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")


object IndexView extends HtmlPage:
  override def pageContent = div(
    p("Welcome!"),
    a(href := "/hello/Bob")("Hello world")
  )

class HelloView(name: String) extends HtmlPage:
  override def pageContent =
    div("Hello ", b(name), "!")
