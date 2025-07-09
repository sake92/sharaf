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
{% include "query_params.sc" %}
```

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