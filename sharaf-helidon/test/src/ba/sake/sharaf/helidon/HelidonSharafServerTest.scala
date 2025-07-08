package ba.sake.sharaf.helidon

import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

class HelidonSharafServerTest extends munit.FunSuite {

  val routes = Routes { case GET -> Path("hello") =>
    Response.withBody("Hello World!")
  }
  val port = NetworkUtils.getFreePort()
  val server = HelidonSharafServer("localhost", port, SharafHandler.routes(routes))

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Hello") {
    val res = quickRequest.get(uri"http://localhost:${port}/hello").send()
    assertEquals(res.body, "Hello World!")
  }
}
