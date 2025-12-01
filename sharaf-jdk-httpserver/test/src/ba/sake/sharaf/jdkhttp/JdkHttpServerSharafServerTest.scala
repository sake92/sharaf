package ba.sake.sharaf.jdkhttp

import sttp.client4.quick.*
import sttp.model.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

class JdkHttpServerSharafServerTest extends munit.FunSuite {

  val routes = Routes { case GET -> Path("hello") =>
    Response.withBody("Hello World!")
  }
  val port = NetworkUtils.getFreePort()
  val server = JdkHttpServerSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Hello") {
    val res = quickRequest.get(uri"http://localhost:${port}/hello").send()
    assertEquals(res.body, "Hello World!")
  }
  test("Not found") {
    val res = quickRequest.get(uri"http://localhost:${port}/notfound").send()
    assertEquals(res.code, StatusCode.NotFound)
  }
}
