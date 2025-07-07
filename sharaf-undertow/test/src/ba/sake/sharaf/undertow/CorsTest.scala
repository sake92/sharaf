package ba.sake.sharaf.undertow

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.utils.*

class CorsTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"
  val routes = Routes { case GET -> Path("cors") =>
    Response.withBody("CORS")
  }

  val server = UndertowSharafServer(
    "localhost",
    port,
    routes,
    corsSettings = CorsSettings.default.withAllowedOrigins(Set("http://example.com"))
  )

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("CORS should work") {
    locally {
      // localhost always works
      val res = quickRequest.get(uri"${baseUrl}/cors").send()
      assertEquals(res.code, StatusCode.Ok)
    }
    locally {
      // allowed origin is allowed
      val res = quickRequest.get(uri"${baseUrl}/cors").headers(Map(HeaderNames.Origin -> "http://example.com")).send()
      assertEquals(res.headers(HeaderNames.AccessControlAllowOrigin), Seq("http://example.com"))
    }
    locally {
      // forbidden origin is not allowed (to browser)
      val res = quickRequest.get(uri"${baseUrl}/cors").headers(Map(HeaderNames.Origin -> "http://example2.com")).send()
      assertEquals(res.headers(HeaderNames.AccessControlAllowOrigin), Seq.empty)
    }
  }
}
