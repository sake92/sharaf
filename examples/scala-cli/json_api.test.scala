//> using scala "3.7.0"
//> using dep ba.sake::tupson:0.18.0
//> using dep com.softwaremill.sttp.client4::core::4.0.13
//> using test.dep org.scalameta::munit::1.2.1

import sttp.client4.quick.*
import ba.sake.tupson.*

case class Car(brand: String, model: String, quantity: Int) derives JsonRW

class JsonApiSuite extends munit.FunSuite {

  val baseUrl = "http://localhost:8181"

  test("create and get cars") {
    locally {
      val res = quickRequest.get(uri"${baseUrl}/cars").send()
      val resBody = res.body.parseJson[Seq[Car]]
      assertEquals(res.code.code, 200)
      assertEquals(res.headers("content-type"), Seq("application/json; charset=utf-8"))
      assertEquals(resBody, Seq.empty)
    }

    locally {
      val body = Car("Mercedes", "ML350", 1)
      val res = quickRequest.post(uri"$baseUrl/cars").body(body.toJson).send()
      assertEquals(res.code.code, 200)
    }

    locally {
      val res = quickRequest.get(uri"$baseUrl/cars/Mercedes").send()
      val resBody = res.body.parseJson[Seq[Car]]
      assertEquals(res.code.code, 200)
      assertEquals(res.headers("content-type"), Seq("application/json; charset=utf-8"))
      assertEquals(resBody, Seq(Car("Mercedes", "ML350", 1)))
    }
  }
}
