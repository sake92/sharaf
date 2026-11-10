package ba.sake.sharaf.handlers

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*

abstract class AbstractCorsTest extends munit.FunSuite with ServerFixture {

  for origin <- Seq(None, Some("http://example.com"), Some("http://example2.com")) do {
    test(s"CORS response for origin $origin") {
      val routes = Routes { case GET -> Path("cors") => Response.withBody("CORS") }
      val settings = CorsSettings.default.withAllowedOrigins(Set("http://example.com"))
      val handler = SharafHandler.cors(SharafHandler.routes(routes), settings)
      withHandler(handler) { base =>
        val response = request
          .get(uri"$base/cors")
          .headers(origin.map(value => HeaderNames.Origin -> value).toMap)
          .send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body, "CORS")
        val allowed = origin.filter(_ == "http://example.com").toSeq
        assertEquals(response.headers(HeaderNames.AccessControlAllowOrigin), allowed)
      }
    }
  }
}
