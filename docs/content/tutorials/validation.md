---
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

---

Create a file `validation.sc` and paste this code into it:

```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("cars") =>
    val qp = Request.current.queryParamsValidated[CarQuery]
    Response.withBody(CarApiResult(s"Query OK: ${qp}"))

  case POST -> Path("cars") =>
    val json = Request.current.bodyJsonValidated[Car]
    Response.withBody(CarApiResult(s"JSON body OK: ${json}"))

UndertowSharafServer("localhost", 8181, routes)
  .withExceptionMapper(ExceptionMapper.json)
  .start()

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

Then run it like this:
```sh
scala-cli validation.sc 
```

Notice above that we used `queryParamsValidated` and not plain `queryParams` (does not validate query params).  
Also, for JSON body parsing+validation we use `bodyJsonValidated` and not plain `bodyJson` (does not validate JSON body).  

---
When you do a GET [http://localhost:8181/cars?brand=  ](http://localhost:8181/cars?brand=  )  
you will get a nice JSON error message with HTTP Status of `400 Bad Request`:
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
    "status": 400
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
    "status": 400
}
```
