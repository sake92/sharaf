package ba.sake.sharaf.helidon

import ba.sake.sharaf.*
import ba.sake.sharaf.helidon.*

class HelidonSharafServerTest extends munit.FunSuite {

  val routes = HelidonSharafRoutes { case GET -> Path("hello") =>
    Response.withBody("Hello World!")
  }
  val port = 8080
  val server = HelidonSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Hello") {
    val res = requests.get(s"http://localhost:8080/hello")
    assertEquals(res.text(), "Hello World!")
  }
}
