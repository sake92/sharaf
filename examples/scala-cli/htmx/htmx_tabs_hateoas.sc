//> using scala "3.3.1"
//> using dep ba.sake::sharaf:0.0.22

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*, routing.*

object IndexView extends HtmlPage with HtmxDependencies:
  override def bodyContent =
    div(id := "tabs", hx.get := "/tab1", hx.trigger := "load delay:100ms", hx.target := "#tabs", hx.swap := "innerHTML")

def tabSnippet(tabNum: Int) = div(
  div(
    cls := "tab-list",
    button(hx.get := "/tab1", Option.when(tabNum == 1)(cls := "selected"), "Tab 1"),
    button(hx.get := "/tab2", Option.when(tabNum == 2)(cls := "selected"), "Tab 2"),
    button(hx.get := "/tab3", Option.when(tabNum == 3)(cls := "selected"), "Tab 3")
  ),
  div(id := "tab-content", cls := "tab-content")(s"TAB ${tabNum} content ....")
)

val routes = Routes:
  case GET() -> Path() =>
    Response.withBody(IndexView)
  case GET() -> Path("tab1") =>
    Response.withBody(tabSnippet(1))
  case GET() -> Path("tab2") =>
    Response.withBody(tabSnippet(2))
  case GET() -> Path("tab3") =>
    Response.withBody(tabSnippet(3))

Undertow.builder
  .addHttpListener(8181, "localhost")
  .setHandler(SharafHandler(routes))
  .build
  .start()

println(s"Server started at http://localhost:8181")
