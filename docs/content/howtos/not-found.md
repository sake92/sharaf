---
layout: howto.html
title: NotFound
description: Sharaf How To NotFound
---

# {{ page.title }}

How to customize 404 NotFound handler?


Use the `notFoundHandler` parameter of `UndertowSharafServer`:
```scala
val customNotFoundHandler: SharafHandler = _ =>
  Response.withBody("Page not found")
    .withStatus(StatusCode.NotFound)

val server = UndertowSharafServer(
    "localhost",
    port,
    routes,
    notFoundHandler = customNotFoundHandler
  )
```

The handler receives a `RequestContext` if the response depends on the request.
