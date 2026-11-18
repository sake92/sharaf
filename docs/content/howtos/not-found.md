---
layout: howto.html
title: NotFound
description: Sharaf How To NotFound
---

# {{ page.title }}

How to customize 404 NotFound handler?


Use the `notFoundHandler` parameter of `PotatehSharafServer`:
```scala
import ba.sake.sharaf.potateh.PotatehSharafServer

val customNotFoundHandler: SharafHandler = _ =>
  Response.withBody("Page not found")
    .withStatus(StatusCode.NotFound)

val server = PotatehSharafServer(
    routes, host = "localhost", port = port,
    notFoundHandler = customNotFoundHandler
  )
server.start()
```

The handler receives a `RequestContext` if the response depends on the request.
