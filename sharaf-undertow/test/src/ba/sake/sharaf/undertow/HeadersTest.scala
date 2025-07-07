package ba.sake.sharaf.undertow

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

class HeadersTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("settingHeader") =>
      Response.settingHeader("header1", "header1Value")
    case GET -> Path("removingHeader") =>
      Response.settingHeader("bla", "bla1").removingHeader("bla")
    case GET -> Path("setAndRemove") =>
      Response.settingHeader("header1", "header1Value").removingHeader("header1")
  }

  val server = UndertowSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("settingHeader sets a header") {
    val res = quickRequest.get(uri"${baseUrl}/settingHeader").send()
    assertEquals(res.headers("header1"), Seq("header1Value"))
  }

  test("removingHeader removes a header") {
    val res = quickRequest.get(uri"${baseUrl}/removingHeader").send()
    assertEquals(res.headers("bla"), Seq.empty)
  }

  test("settingHeader and then removingHeader removes a header") {
    val res = quickRequest.get(uri"${baseUrl}/setAndRemove").send()
    assertEquals(res.headers("header1"), Seq.empty)
  }
}
