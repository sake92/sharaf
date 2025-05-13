package ba.sake.sharaf.undertow.handlers

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.*
import ba.sake.sharaf.utils.NetworkUtils

class SharafHandlerTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes { case GET -> Path("hello") =>
    Response.withBody("hello")
  }

  val server = UndertowSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  // This returns a 403 because of
  // https://github.com/undertow-io/undertow/blob/42993e8d2c787541bb686fb97b13bea4649d19bb/core/src/main/java/io/undertow/server/handlers/resource/ResourceHandler.java#L236
  // Need to manually handle empty Path()
  test("/ returns a 404".ignore) {
    assertEquals(quickRequest.get(uri"${baseUrl}").send().code, StatusCode.NotFound)
    assertEquals(quickRequest.get(uri"${baseUrl}/").send().code, StatusCode.NotFound)
  }

  test("/does-not-exist returns a 404") {
    val res = quickRequest.get(uri"${baseUrl}/does-not-exist").send()
    assertEquals(res.code, StatusCode.NotFound)
    assertEquals(res.body, "Not Found")
  }

  test("/hello returns a string") {
    val res = quickRequest.get(uri"${baseUrl}/hello").send()
    assertEquals(res.body, "hello")
  }
}
