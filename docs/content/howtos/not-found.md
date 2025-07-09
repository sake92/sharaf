---
title: NotFound
description: Sharaf How To NotFound
---

# {{ page.title }}

How to customize 404 NotFound handler?


Use the `notFoundHandler` parameter of `UndertowSharafServer`:
```scala
val customNotFoundHandler: Request => Response[?] = req =>
  Response.withBody(MyCustomNotFoundPage)
    .withStatus(StatusCode.NotFound)

val server = UndertowSharafServer(
    "localhost",
    port,
    routes,
    notFoundHandler = customNotFoundHandler
  )
```

You can use the request if you need to dynamically decide on what to return.  
Or ignore it and return a static not found response.

