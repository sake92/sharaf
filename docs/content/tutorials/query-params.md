---
layout: tutorial.html
title: Query Params
description: Sharaf Tutorial Query Params
---

# {{ page.title }}

### Raw
Raw query parameters can be accessed through `Request.current.queryParamsRaw`.  
This is a `Map[String, Seq[String]]` which you can use to extract query parameters.  
The raw approach is useful for simple cases and dynamic query parameters.

### Typed
For more type safety you can use the `QueryStringRW` typeclass.  
Make a `case class MyParams(..) derives QueryStringRW`  
and then use it like this: `Request.current.queryParams[MyParams]`

---

Create a file `query_params.sc` and paste this code into it:
<!-- example:start examples/scala-cli/query_params.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("raw") =>
    val qp = Request.current.queryParamsRaw
    Response.withBody(s"raw params = ${qp}")

  case GET -> Path("typed") =>
    case class SearchParams(q: String, perPage: Int) derives QueryStringRW
    val qp = Request.current.queryParams[SearchParams]
    Response.withBody(s"typed params = ${qp}")

  case GET -> _ =>
    Response.withBody("Try http://localhost:8181/raw?q=scala&perPage=10 or http://localhost:8181/typed?q=scala&perPage=10")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")
```
<!-- example:end -->

Then run it like this:
```sh
scala query_params.sc 
```

---
Now go to http://localhost:8181/raw?q=what&perPage=10
and you will get the raw query params map:
```
params = Map(perPage -> List(10), q -> List(what))
```

and if you go to http://localhost:8181/typed?q=what&perPage=10
you will get a type-safe, parsed query params object:
```
params = SearchParams(what,10)
```

The [request reference](/reference/request.html) lists the supported query parsing types.
