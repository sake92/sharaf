package api

import scala.compiletime.uninitialized
import sttp.model.*
import sttp.client4.quick.*
import ba.sake.querson.*
import ba.sake.tupson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*
import ba.sake.sharaf.utils.*

class JsonApiSuite extends munit.FunSuite {

  val moduleFixture = new Fixture[JsonApiModule]("JsonApiModule") {
    private var module: JsonApiModule = uninitialized

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = JsonApiModule(NetworkUtils.getFreePort())
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

  override def munitFixtures = List(moduleFixture)

  test("products can be created and fetched") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl

    // first GET -> empty
    locally {
      val res = quickRequest.get(uri"$baseUrl/products").send()
      assertEquals(res.code, StatusCode.Ok)
      assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
      assertEquals(res.body.parseJson[Seq[ProductRes]], Seq.empty)
    }

    // create a few products
    val firstProduct = locally {
      val reqBody = CreateProductReq.of("Chocolate", 5)
      val res =
        quickRequest
          .post(uri"$baseUrl/products")
          .body(reqBody.toJson)
          .headers(Map("Content-Type" -> "application/json; charset=utf-8"))
          .send()
      assertEquals(res.code, StatusCode.Ok)
      assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
      val resBody = res.body.parseJson[ProductRes]
      assertEquals(resBody.name, "Chocolate")
      assertEquals(resBody.quantity, 5)

      resBody
    }

    // add second one
    quickRequest
      .post(uri"$baseUrl/products")
      .body(CreateProductReq.of("Milk", 7).toJson)
      .headers(Map("Content-Type" -> "application/json; charset=utf-8"))
      .send()

    // second GET -> new product
    locally {
      val res = quickRequest.get(uri"$baseUrl/products").send()
      assertEquals(res.code, StatusCode.Ok)
      assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
      val resBody = res.body.parseJson[Seq[ProductRes]]
      assertEquals(resBody.size, 2)
      assertEquals(resBody.head.name, "Chocolate")
      assertEquals(resBody.head.quantity, 5)
    }

    // filtering GET
    // TODO reenable
    locally {
      val queryParams = ProductsQuery(Set("Chocolate"), Option(1)).toSttpQuery()
      val res = quickRequest.get(uri"$baseUrl/products".withParams(queryParams)).send()
      assertEquals(res.code, StatusCode.Ok)
      assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
      val resBody = res.body.parseJson[Seq[ProductRes]]
      assertEquals(resBody.size, 1)
      assertEquals(resBody.head.name, "Chocolate")
      assertEquals(resBody.head.quantity, 5)
    }

    // GET by id
    locally {
      val res = quickRequest.get(uri"$baseUrl/products/${firstProduct.id}").send()
      assertEquals(res.code, StatusCode.Ok)
      assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
      val resBody = res.body.parseJson[ProductRes]
      assertEquals(resBody, firstProduct)
    }
  }

  test("400 BadRequest when query params not valid") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    val res = quickRequest.get(uri"$baseUrl/products?minQuantity=not_a_number").send()
    val resProblem = res.body.parseJson[ProblemDetails]
    assertEquals(res.code, StatusCode.BadRequest)
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

  test("422 UnprocessableEntity when body not valid") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl

    // blank name not allowed
    val reqBody = """{
      "name": "  ",
      "quantity": 0
    }"""
    val res =
      quickRequest
        .post(uri"$baseUrl/products")
        .body(reqBody)
        .headers(Map("Content-Type" -> "application/json; charset=utf-8"))
        .send()

    val resProblem = res.body.parseJson[ProblemDetails]

    assertEquals(res.code, StatusCode.UnprocessableEntity)
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

}
