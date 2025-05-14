//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// https://htmx.org/examples/lazy-load/
// scala htmx_lazy_load.sc --resource-dir resources

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case GET -> Path("graph") =>
    Thread.sleep(1000) // simulate slow, stonks
    val graph = img(src := "/img/tokyo.png")
    Response.withBody(graph)

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

object views {
  def IndexView = doctype("html")(
    html(
      head(
        tag("style")("""
          .htmx-settling img {
            opacity: 0;
          }
          img {
            transition: opacity 300ms ease-in;
            width: 400px;
          }
        """),
        script(src := "https://unpkg.com/htmx.org@2.0.4")
      ),
      body(
        div(hx.get := "/graph", hx.trigger := "load")(
          img(src := "/img/bars.svg", alt := "Result loading...", cls := "htmx-indicator")
        )
      )
    )
  )
}
