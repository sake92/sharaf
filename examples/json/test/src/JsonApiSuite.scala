package demo

import ba.sake.querson.*
import ba.sake.tupson.*
import scala.util.Random
import io.undertow.Undertow

class JsonApiSuite extends munit.FunSuite {

  override def munitFixtures = List(serverFixture)

  test("customers can be created and fetched") {
    val server = serverFixture()
    val serverInfo = server.getListenerInfo().get(0)
    val baseUrl = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"

    // first GET -> empty
    locally {
      val res = requests.get(s"$baseUrl/customers")
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      assertEquals(res.text.parseJson[Seq[CustomerRes]], Seq.empty)
    }

    // create a few customers
    val firstCustomer = locally {
      val reqBody = CreateCustomerReq("Meho", CreateAddressReq("nizbrdo"))
      val res =
        requests.post(s"$baseUrl/customers", data = reqBody.toJson, headers = Map("Content-Type" -> "application/json"))
      assertEquals(res.statusCode, 200)
      assertEquals(res.headers("content-type"), Seq("application/json"))
      val resBody = res.text.parseJson[CustomerRes]
      assertEquals(resBody.name, "Meho")
      assertEquals(resBody.address, AddressRes("nizbrdo"))

      // add second one
      requests.post(
        s"$baseUrl/customers",
        data = CreateCustomerReq("Hamo", CreateAddressReq("tamo")).toJson,
        headers = Map("Content-Type" -> "application/json")
      )

      resBody
    }

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

  // TODO extract into a sharaf-test module
  // which users would add to their test-classpath
  val serverFixture = new Fixture[Undertow]("JsonApiServer") {
    private var underlyingServer: Undertow = _
    def apply() = underlyingServer
    override def beforeEach(context: BeforeEach): Unit =
      underlyingServer = JsonApiServer(Random.between(1_024, 65_535)).server
      underlyingServer.start()
    override def afterEach(context: AfterEach): Unit =
      underlyingServer.stop
  }

}
