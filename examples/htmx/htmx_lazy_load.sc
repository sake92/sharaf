//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.13.0

// https://htmx.org/examples/lazy-load/

import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(views.IndexView)
  case GET -> Path("graph") =>
    Thread.sleep(1000) // simulate slow, stonks
    val graph = html""" <img src="/img/tokyo.png" """
    Response.withBody(graph)

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

object views {
  def IndexView =
    html"""
    <!DOCTYPE html>
    <html>
    <head>
        <script src="https://unpkg.com/htmx.org@2.0.4"></script>
        <style>
        .htmx-settling img {
            opacity: 0;
        }
        img {
            transition: opacity 300ms ease-in;
            width: 400px;
        }
        </style>
    </head>
    <body>
        <div hx-get="/graph" hx-trigger="load">
            <img src="/img/bars.svg" alt="Result loading..." class="htmx-indicator">
        </div>
    </body>
    </html>
    """
}
