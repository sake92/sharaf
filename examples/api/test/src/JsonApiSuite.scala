package api

import scala.compiletime.uninitialized
import ba.sake.querson.*
import ba.sake.tupson.*
import ba.sake.sharaf.exceptions.*
import ba.sake.sharaf.utils.*

class JsonApiSuite extends munit.FunSuite {

  override def munitFixtures = List(moduleFixture)

  test("products can be created and fetched") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl

    // first GET -> empty
    locally {
      val res = requests.get(s"$baseUrl/products")
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      assertEquals(res.text.parseJson[Seq[ProductRes]], Seq.empty)
    }

    // create a few products
    val firstProduct = locally {
      val reqBody = CreateProductReq.of("Chocolate", 5)
      val res =
        requests.post(s"$baseUrl/products", data = reqBody.toJson, headers = Map("Content-Type" -> "application/json"))
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[ProductRes]
      assertEquals(resBody.name, "Chocolate")
      assertEquals(resBody.quantity, 5)

      resBody
    }

    // add second one
    requests.post(
      s"$baseUrl/products",
      data = CreateProductReq.of("Milk", 7).toJson,
      headers = Map("Content-Type" -> "application/json")
    )

    // second GET -> new product
    locally {
      val res = requests.get(s"$baseUrl/products")
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[Seq[ProductRes]]
      assertEquals(resBody.size, 2)
      assertEquals(resBody.head.name, "Chocolate")
      assertEquals(resBody.head.quantity, 5)
    }

    // filtering GET
    locally {
      val queryParams = ProductsQuery(Set("Chocolate"), Option(1)).toRequestsQuery()
      val res = requests.get(s"$baseUrl/products", params = queryParams)
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[Seq[ProductRes]]
      assertEquals(resBody.size, 1)
      assertEquals(resBody.head.name, "Chocolate")
      assertEquals(resBody.head.quantity, 5)
    }

    // GET by id
    locally {
      val res = requests.get(s"$baseUrl/products/${firstProduct.id}")
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[ProductRes]
      assertEquals(resBody, firstProduct)
    }
  }

  test("400 BadRequest when query params not valid") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    val ex = intercept[requests.RequestFailedException] {
      requests.get(s"$baseUrl/products?minQuantity=not_a_number")
    }
    val resProblem = ex.response.text().parseJson[ProblemDetails]

    assertEquals(ex.response.statusCode, 400)
    assert(
      resProblem.invalidArguments.contains(
        ProblemDetails.ArgumentProblem(
          "minQuantity[0]",
          "invalid Int",
          Some("not_a_number")
        )
      )
    )
  }

  test("400 BadRequest when body not valid") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl

    // blank name not allowed
    val reqBody = """{
      "name": "  ",
      "quantity": 0
    }"""
    val ex = intercept[requests.RequestFailedException] {
      requests.post(s"$baseUrl/products", data = reqBody, headers = Map("Content-Type" -> "application/json"))
    }
    val resProblem = ex.response.text().parseJson[ProblemDetails]

    assertEquals(ex.response.statusCode, 400)
    println(resProblem.invalidArguments)
    assert(
      resProblem.invalidArguments.contains(
        ProblemDetails.ArgumentProblem(
          "$.name",
          "must not be blank",
          Some("  ")
        )
      )
    )
  }

  val moduleFixture = new Fixture[JsonApiModule]("JsonApiModule") {
    private var module: JsonApiModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = JsonApiModule(getFreePort())
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

}
