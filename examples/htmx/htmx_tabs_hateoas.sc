//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

// scala htmx_tabs_hateoas.sc --resource-dir resources

import io.undertow.Undertow
import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("tab1") =>
    Response.withBody(tabSnippet(1))
  case GET -> Path("tab2") =>
    Response.withBody(tabSnippet(2))
  case GET -> Path("tab3") =>
    Response.withBody(tabSnippet(3))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")


def IndexView = doctype("html")(
  html(
    head(
      script(src := "https://unpkg.com/htmx.org@2.0.4")
    ),
    body(
      div(
        id := "tabs",
        hx.get := "/tab1",
        hx.trigger := "load delay:100ms",
        hx.target := "#tabs",
        hx.swap := "innerHTML"
      )
    )
  )
)

def tabSnippet(tabNum: Int) = div(
  div(
    cls := "tab-list",
    button(hx.get := "/tab1", Option.when(tabNum == 1)(cls := "selected"), "Tab 1"),
    button(hx.get := "/tab2", Option.when(tabNum == 2)(cls := "selected"), "Tab 2"),
    button(hx.get := "/tab3", Option.when(tabNum == 3)(cls := "selected"), "Tab 3")
  ),
  div(id := "tab-content", cls := "tab-content")(s"TAB ${tabNum} content ....")
)
