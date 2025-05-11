package ba.sake.sharaf.undertow.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.*

class SharafHandlerTest extends munit.FunSuite {

  val port = utils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = UndertowSharafRoutes {
    case GET -> Path("hello") =>
      Response.withBody("hello")
  }

  val server = UndertowSharafServer("localhost", port,routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  // This returns a 403 because of
  // https://github.com/undertow-io/undertow/blob/42993e8d2c787541bb686fb97b13bea4649d19bb/core/src/main/java/io/undertow/server/handlers/resource/ResourceHandler.java#L236
  // Need to manually handle empty Path()
  test("/ returns a 404".ignore) {
    assertEquals(requests.get(s"${baseUrl}", check = false).statusCode, 404)
    assertEquals(requests.get(s"${baseUrl}/", check = false).statusCode, 404)
  }

  test("/does-not-exist returns a 404") {
    val res = requests.get(s"${baseUrl}/does-not-exist", check = false)
    assertEquals(res.statusCode, 404)
    assertEquals(res.text(), "Not Found")
  }
  
  test("/hello returns a string") {
    val res = requests.get(s"${baseUrl}/hello")
    assertEquals(res.text(), "hello")
  }
}
