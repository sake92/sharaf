---
layout: howto.html
title: Exception Handler
description: Sharaf How To Exception Handler
---

# {{ page.title }}

How to customize the Exception handler?

Use the `exceptionMapper` parameter of `PotatehSharafServer.start`:
```scala
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.potateh.PotatehSharafServer

val customExceptionMapper: ExceptionMapper = {
  case e: MyException =>
    val errorPage = MyErrorPage(e.getMessage())
    Response.withBody(errorPage)
      .withStatus(StatusCode.InternalServerError)
}
val finalExceptionMapper = customExceptionMapper.orElse(ExceptionMapper.default)

val server = PotatehSharafServer.start(
    ServerConfig.Default.withBindAddress(new java.net.InetSocketAddress("localhost", port)),
    routes,
    exceptionMapper = finalExceptionMapper
  )
```

The `ExceptionMapper` is a partial function from an exception to `Response`.  
Here we need to chain our custom exception mapper before the default one.

