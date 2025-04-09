package ba.sake.sharaf.handlers


import io.undertow.Undertow
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.utils

class SharafHandlerTest extends munit.FunSuite {

  val port = utils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("hello") =>
      Response.withBody("hello")
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  // TODO fix this ???
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
