package ba.sake.sharaf.undertow

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.utils.*

class WebJarsTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"
  val routes = Routes { case GET -> Path() =>
    Response.withBody("WebJars!")
  }
  
  val server = UndertowSharafServer("localhost", port, routes)
  
  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()
  
   // WebJars
  test("WebJars should work") {
    val res = quickRequest.get(uri"${baseUrl}/jquery/3.7.1/jquery.js").send()
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/javascript"))
    assert(res.body.length > 100)
  }
}
