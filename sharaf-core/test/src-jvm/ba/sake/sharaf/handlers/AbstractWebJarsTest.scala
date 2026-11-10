package ba.sake.sharaf.handlers

import sttp.client4.quick.*
import sttp.model.{HeaderNames, StatusCode}
import ba.sake.sharaf.*

abstract class AbstractWebJarsTest extends munit.FunSuite {
  def withRoutesServer(routes: Routes)(check: String => Unit): Unit

  test("routes constructor serves a WebJar") {
    val routes = Routes { case GET -> Path() => Response.withBody("WebJars!") }
    withRoutesServer(routes) { base =>
      val response = quickRequest.get(uri"$base/jquery/4.0.0/jquery.js").send()
      assertEquals(response.code, StatusCode.Ok)
      assert(response.body.length > 100)
      assertEquals(response.headers(HeaderNames.ContentType), Seq("text/javascript"))
    }
  }
}
