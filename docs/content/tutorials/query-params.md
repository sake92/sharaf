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

## Named Tuples
You can also use named tuples to parse query params: `Request.current.queryParams[(q: String, perPage: Int)]`.
In this case you don't even need to define a separate class!

> Note that you can't use [Validation](/tutorials/validation.html) with named tuples

## Union Types
Union Types are also handy sometimes.
Say you have a filter functionality, where a user can filter by "firstName" or by "lastName".
You can write: `Request.current.queryParams[(firstName: String) | (lastName: String)]`.

Here we are combining 2 named tuples together with into a union type.
You could use any other type of course.

---

You can also write `Request.current.queryParams[(id: Int | String)]`, but we would not recommend it.  
Of course, if you need it, go for it!

In this case, it will first try to parse an `Int` and if that fails it will parse a `String`.

{% include "tutorial_nav.html" %}
