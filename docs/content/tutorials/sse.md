---
title: Server Sent Events
description: Sharaf Server Sent Events
---

# {{ page.title }}

[HTMX]("https://htmx.org/") is an incredibly simple, HTML-first library.  
Instead of going through HTML->JS->JSON-API loop/mess, you can go directly HTML->HTML-API.  
Basically you just return HTML snippets that get included where you want in your page.

You can lots of examples in [examples/htmx]({{site.data.project.gh.sourcesUrl}}/examples/htmx) folder.

---

Let's make a simple page that sends 5 SSE events and then a "stop" message.  
Create a file `sse.sc` and paste this code into it:

```scala
//> using scala 3.7.3
//> using dep ba.sake::sharaf-undertow:0.14.0

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
        </head>
        <body hx-ext="sse">
        <div>
            <h1>Hello Sharaf SSE!</h1>
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
            data = html"""<div>event${i}</div>""".toString
          )
        )
        Thread.sleep(1_000)
      sseSender.send(ServerSentEvent.Done())
    }).start()
    Response.withBody(sseSender)
}

UndertowSharafServer("localhost", 8181, routes).start()
println(s"Server started at http://localhost:8181")
```

and run it like this:
```sh
scala sse.sc 
```

Go to [http://localhost:8181](http://localhost:8181)  
to see how it works.

---
In this example we have used HTMX for the frontend.  
It has a nice functionality where it stops on a "stop" message.  
Otherwise the browser would constantly try to reconnect.

Of course, the sending of events is usually much more complicated.  
The coordination of threads and which events to send to which browser is on you to implement.
