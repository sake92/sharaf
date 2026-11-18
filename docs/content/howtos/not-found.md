---
layout: howto.html
title: NotFound
description: Sharaf How To NotFound
---

# {{ page.title }}

How to customize 404 NotFound handler?


Use the `notFoundHandler` parameter of `PotatehSharafServer.start`:
```scala
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.potateh.PotatehSharafServer

val customNotFoundHandler: SharafHandler = _ =>
  Response.withBody("Page not found")
    .withStatus(StatusCode.NotFound)

val server = PotatehSharafServer.start(
    ServerConfig.Default.withBindAddress(new java.net.InetSocketAddress("localhost", port)),
    routes,
    notFoundHandler = customNotFoundHandler
  )
```

The handler receives a `RequestContext` if the response depends on the request.
