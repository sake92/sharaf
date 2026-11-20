---
layout: tutorial.html
title: Validation
description: Sharaf Tutorial Validation
---

# {{ page.title }}



For validating data you need to use the `Validator` typeclass.  
A small example:

```scala
import ba.sake.validson.Validator

case class ValidatedData(num: Int, str: String, seq: Seq[String])

object ValidatedData:
    given Validator[ValidatedData] = Validator
    .derived[ValidatedData]
    .positive(_.num)
    .notBlank(_.str)
    .minItems(_.seq, 1)
```

The `ValidatedData` can be any `case class`: json data, form data, query params..  

`Validator` also provides `.notEmpty`, `.email`, and `.exactLength` for strings. For collections, use
`.allItems(_.values, predicate, message)` when every element must meet a condition. For example:

```scala
.email(_.contactEmail)
.allItems(_.tags, _.nonEmpty, "must not be empty")
```

Derived validators recurse through nested `Seq` and `Option` fields when their element type has a `Validator`.
`None` is valid; use a non-optional field when the value is required.

---

Create a file `validation.sc` and paste this code into it:

<!-- example:start examples/scala-cli/validation.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("cars") =>
    val qp = Request.current.queryParamsValidated[CarQuery]
    Response.withBody(CarApiResult(s"Query OK: ${qp}"))

  case POST -> Path("cars") =>
    val json = Request.current.bodyJsonValidated[Car]
    Response.withBody(CarApiResult(s"JSON body OK: ${json}"))

PotatehSharafServer(routes, host = "localhost", port = 8181, exceptionMapper = ExceptionMapper.json).start()

println(s"Server started at http://localhost:8181")

case class Car(brand: String, model: String, quantity: Int) derives JsonRW
object Car:
  given Validator[Car] = Validator
    .derived[Car]
    .notBlank(_.brand)
    .notBlank(_.model)
    .nonNegative(_.quantity)

case class CarQuery(brand: String) derives QueryStringRW
object CarQuery:
  given Validator[CarQuery] = Validator
    .derived[CarQuery]
    .notBlank(_.brand)

case class CarApiResult(message: String) derives JsonRW
```
<!-- example:end -->

Then run it like this:
```sh
scala validation.sc 
```

Notice above that we used `queryParamsValidated` and not plain `queryParams` (does not validate query params).  
Also, for JSON body parsing+validation we use `bodyJsonValidated` and not plain `bodyJson` (does not validate JSON body).  

---
When you do a GET [http://localhost:8181/cars?brand=  ](http://localhost:8181/cars?brand=  )  
you will get a JSON error message with HTTP status `422 Unprocessable Entity`:
```json
{
    "instance": null,
    "invalidArguments": [
    {
        "reason": "must not be blank",
        "path": "$.brand",
        "value": ""
    }
    ],
    "detail": "",
    "type": null,
    "title": "Validation errors",
    "status": 422
}
```

The error message format follows the [RFC 7807 problem detail](https://datatracker.ietf.org/doc/html/rfc7807).

---

When you do a POST [http://localhost:8181/cars](http://localhost:8181/cars) with a malformed body:
```json
{
    "brand": " ",
    "model": "ML350",
    "quantity": -5
}
```

you will get these errors:
```json
{
    "instance": null,
    "invalidArguments": [
    {
        "reason": "must not be blank",
        "path": "$.brand",
        "value": " "
    },
    {
        "reason": "must not be negative",
        "path": "$.quantity",
        "value": "-5"
    }
    ],
    "detail": "",
    "type": null,
    "title": "Validation errors",
    "status": 422
}
```
