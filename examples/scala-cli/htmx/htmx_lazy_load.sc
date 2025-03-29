//> using scala "3.4.2"
//> using dep ba.sake::sharaf:0.9.0

// https://htmx.org/examples/lazy-load/

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*, routing.*

object IndexView extends HtmlPage with HtmxDependencies:
  override def pageContent = div(hx.get := "/graph", hx.trigger := "load")(
    img(src := "/img/bars.svg", alt := "Result loading...", cls := "htmx-indicator")
  )

  override def stylesInline = List("""
    .htmx-settling img {
      opacity: 0;
    }
    img {
      transition: opacity 300ms ease-in;
      width: 400px;
    }
  """)

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("graph") =>
    Thread.sleep(1000) // simulate slow, stonks
    val graph = img(src := "/img/tokyo.png")
    Response.withBody(graph)

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
