package demo

import ba.sake.sharaf.handlers.ProblemDetails
import ba.sake.sharaf.handlers.ArgumentProblem
import ba.sake.sharaf.SharafUtils
import ba.sake.querson.*
import ba.sake.tupson.*

class JsonApiSuite extends munit.FunSuite {

  override def munitFixtures = List(moduleFixture)

  test("customers can be created and fetched") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl

    // first GET -> empty
    locally {
      val res = requests.get(s"$baseUrl/customers")
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      assertEquals(res.text.parseJson[Seq[CustomerRes]], Seq.empty)
    }

    // create a few customers
    val firstCustomer = locally {
      val reqBody = CreateCustomerReq.of("Meho", CreateAddressReq("nizbrdo"))
      val res =
        requests.post(s"$baseUrl/customers", data = reqBody.toJson, headers = Map("Content-Type" -> "application/json"))
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[CustomerRes]
      assertEquals(resBody.name, "Meho")
      assertEquals(resBody.address, AddressRes("nizbrdo"))

      resBody
    }

    // add second one
    requests.post(
      s"$baseUrl/customers",
      data = CreateCustomerReq.of("Hamo", CreateAddressReq("tamo")).toJson,
      headers = Map("Content-Type" -> "application/json")
    )

    // second GET -> new customers
    locally {
      val res = requests.get(s"$baseUrl/customers")
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[Seq[CustomerRes]]
      assertEquals(resBody.size, 2)
      assertEquals(resBody.head.name, "Meho")
      assertEquals(resBody.head.address, AddressRes("nizbrdo"))
    }

    // filtering GET
    locally {
      val queryParams = UserQuery(Set("Meho")).toQueryStringMap().map { (k, vs) => k -> vs.head }
      val res = requests.get(s"$baseUrl/customers", params = queryParams)
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[Seq[CustomerRes]]
      assertEquals(resBody.size, 1)
      assertEquals(resBody.head.name, "Meho")
      assertEquals(resBody.head.address, AddressRes("nizbrdo"))
    }

    // GET by id
    locally {
      val res = requests.get(s"$baseUrl/customers/${firstCustomer.id}")
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[CustomerRes]
      assertEquals(resBody, firstCustomer)
    }

  }

  test("400 BadRequest when body not valid") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl

    // blank name not allowed
    val reqBody = """{
      "name": "  ",
      "address": {
        "street": "hm"
      }
    }"""
    val ex = intercept[requests.RequestFailedException] {
      requests.post(s"$baseUrl/customers", data = reqBody, headers = Map("Content-Type" -> "application/json"))
    }
    val resProblem = ex.response.text().parseJson[ProblemDetails]

    assertEquals(ex.response.statusCode, 400)
    assert(
      resProblem.invalidArguments.contains(
        ArgumentProblem(
          "$.name",
          "must not be blank",
          Some("  ")
        )
      )
    )
    assert(
      resProblem.invalidArguments.contains(
        ArgumentProblem(
          "$.address.street",
          "must be >= 3",
          Some("hm")
        )
      )
    )

  }

  val moduleFixture = new Fixture[JsonApiModule]("JsonApiModule") {
    private var module: JsonApiModule = _

    def apply() = module

    override def beforeEach(context: BeforeEach): Unit =
      module = JsonApiModule(SharafUtils.getFreePort())
      module.server.start()

    override def afterEach(context: AfterEach): Unit =
      module.server.stop()
  }

}
