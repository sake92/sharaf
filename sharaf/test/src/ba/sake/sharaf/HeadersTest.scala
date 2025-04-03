package ba.sake.sharaf

import io.undertow.Undertow
import ba.sake.sharaf.routing.*

class HeadersTest extends munit.FunSuite {
  
  val port = utils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("settingHeader") =>
      Response.settingHeader("header1", "header1Value")
    case GET -> Path("removingHeader") =>
      // this one is set by default in the CorsHandler
      Response.removingHeader("access-control-allow-credentials")
    case GET -> Path("setAndRemove") =>
      Response.settingHeader("header1", "header1Value").removingHeader("header1")
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("settingHeader sets a header") {
    val res = requests.get(s"${baseUrl}/settingHeader")
    assertEquals(res.headers("header1"), Seq("header1Value"))
  }
  
  test("removingHeader removes a header") {
    val res = requests.get(s"${baseUrl}/removingHeader")
    assertEquals(res.headers.get("access-control-allow-credentials"), None)
  }

  test("settingHeader and then removingHeader removes a header") {
    val res = requests.get(s"${baseUrl}/setAndRemove")
    assertEquals(res.headers.get("header1"), None)
  }
}
