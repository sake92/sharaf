//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.12.1

import ba.sake.sharaf.{*, given}
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

def IndexView =
  html"""
    <!DOCTYPE html>
    <html>
    <head>
      <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    </head>
    <body>
        <div id="tabs" hx-get="/tab1" hx-trigger="load delay:100ms" hx-target="#tabs" hx-swap="innerHTML">
        </div>
    </body>
    </html>
  """

def tabSnippet(tabNum: Int) =
  html"""
    <div class="tab-list">
        <button hx-get="/tab1" class="${if tabNum == 1 then "selected" else ""}">Tab 1</button>
        <button hx-get="/tab2" class="${if tabNum == 2 then "selected" else ""}">Tab 2</button>
        <button hx-get="/tab3" class="${if tabNum == 3 then "selected" else ""}">Tab 3</button>
        <div id="tab-content" class="tab-content">
            ${s"TAB ${tabNum} content ...."}
        </div>
    </div>
  """
