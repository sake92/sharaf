---
title: Tests
description: Sharaf Tutorial Tests
---

# {{ page.title }}

Tests are essential to any serious software component.  
Writing integration tests with Munit and Requests is straightforward.

Here we are testing the API from the [JSON API tutorial](/tutorials/json.html#routes-definition).  
Create a file `json_api.test.scala` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::tupson:0.13.0
//> using dep com.lihaoyi::requests:0.9.0
//> using test.dep org.scalameta::munit::1.1.1

import ba.sake.tupson.*

case class Car(brand: String, model: String, quantity: Int) derives JsonRW

class JsonApiSuite extends munit.FunSuite {

  val baseUrl = "http://localhost:8181"

  test("create and get cars") {
    locally {
      val res = requests.get(s"$baseUrl/cars")
      val resBody = res.text.parseJson[Seq[Car]]
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json; charset=utf-8"))
      assertEquals(res.text.parseJson[Seq[Car]], Seq.empty)
    }

    locally {
      val body = Car("Mercedes", "ML350", 1)
      val res = requests.post(s"$baseUrl/cars", data = body.toJson)
      assertEquals(res.statusCode, 200)
    }

    locally {
      val res = requests.get(s"$baseUrl/cars/Mercedes")
      val resBody = res.text.parseJson[Seq[Car]]
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json; charset=utf-8"))
      assertEquals(resBody, Seq(Car("Mercedes", "ML350", 1)))
    }
  }
}
```

First run the API server in one shell:
```sh
scala-cli test json_api.sc
```

and then run the tests in another shell:
```sh
scala-cli test json_api.test.scala
```

