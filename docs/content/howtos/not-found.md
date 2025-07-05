---
title: NotFound
description: Sharaf How To NotFound
---

# {{ page.title }}

How to customize 404 NotFound handler?


Use the `withNotFoundHandler` on `UndertowSharafServer`:
```scala
UndertowSharafServer(routes).withNotFoundHandler { req =>
  Response.withBody(MyCustomNotFoundPage)
    .withStatus(StatusCode.NotFound)
}
```

The `withNotFoundHandler` accepts a `Request => Response[?]` parameter.  
You can use the request if you need to dynamically decide on what to return.  
Or ignore it and return a static not found response.

