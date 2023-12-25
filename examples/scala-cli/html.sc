//> using scala "3.3.1"
//> using dep ba.sake::sharaf:0.0.17

import io.undertow.Undertow
import ba.sake.hepek.html.HtmlPage
import ba.sake.hepek.scalatags.all.*
import ba.sake.sharaf.*, routing.*

class HelloView(name: String) extends HtmlPage:
  override def bodyContent =
    div("Hello ", b(name), "!")

val routes = Routes:
  case GET() -> Path("hello", name) =>
    Response.withBody(HelloView(name))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
