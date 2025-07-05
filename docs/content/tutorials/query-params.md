---
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
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import ba.sake.querson.QueryStringRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("raw") =>
    val qp = Request.current.queryParamsRaw
    Response.withBody(s"params = ${qp}")

  case GET -> Path("typed") =>
    case class SearchParams(q: String, perPage: Int) derives QueryStringRW
    val qp = Request.current.queryParams[SearchParams]
    Response.withBody(s"params = ${qp}")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
```

Then run it like this:
```sh
scala-cli query_params.sc 
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