//> using scala 3.7.3
//> using dep ba.sake::sharaf-undertow:0.13.3

import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes {
  case GET -> Path() =>
    Response.withBody(
      html"""
        <!DOCTYPE html>
        <html>
        <head>
        <script src="https://unpkg.com/htmx.org@2.0.4"></script>
        <script src="https://unpkg.com/htmx-ext-sse@2.2.4/dist/sse.min.js" ></script>
        <style>
        .fade-me-in.htmx-added {
            opacity: 0;
        }
        .fade-me-in {
            opacity: 1;
            transition: opacity 1s ease-out;
        }
        </style>          
        </head>
        <body hx-ext="sse">
        <div>
            <h1>Hello HTMX + SSE!</h1>
            <div sse-connect="/sse-events" sse-swap="message" sse-close="stop" hx-target="this" hx-swap="beforeend settle:1s"></div>
        </div>
        </body>
        </html>
    """
    )
  case GET -> Path("sse-events") =>
    val sseSender = SseSender()
    new Thread(() => {
      for i <- 1 to 5 do
        sseSender.send(
          ServerSentEvent.Message(
            data = html"""<div class="fade-me-in">event${i}</div>""".toString
          )
        )
        Thread.sleep(1_000)
      sseSender.send(ServerSentEvent.Done())
    }).start()
    Response.withBody(sseSender)
}

UndertowSharafServer("localhost", 8181, routes).start()
println(s"Server started at http://localhost:8181")
