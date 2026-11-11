package ba.sake.sharaf.handlers

import sttp.model.*
import sttp.client4.quick.*
import java.time.Duration
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

  test("CORS only applies to configured path prefixes") {
    val routes = Routes {
      case GET -> Path("api", "hello") => Response.withBody("api")
      case GET -> Path("other")        => Response.withBody("other")
    }
    val settings = CorsSettings.default.withAllowedOrigins(Set("http://example.com")).withPathPrefixes(Set("/api"))
    withHandler(SharafHandler.cors(SharafHandler.routes(routes), settings)) { base =>
      val api = request.get(uri"$base/api/hello").header("Origin", "http://example.com").send(httpBackend)
      val other = request.get(uri"$base/other").header("Origin", "http://example.com").send(httpBackend)
      assertEquals(api.header("Access-Control-Allow-Origin"), Some("http://example.com"))
      assertEquals(other.header("Access-Control-Allow-Origin"), None)
      assertEquals(other.header("Access-Control-Allow-Credentials"), None)
    }
  }

  // sttp 4.0.27's Native curl backend does not send HTTP OPTIONS correctly.
  test("ordinary OPTIONS reaches the route") {
    val routes = Routes { case OPTIONS -> Path("cors") => Response.withBody("ordinary options") }
    val settings = CorsSettings.default.withAllowedOrigins(Set("http://example.com"))
    withHandler(SharafHandler.cors(SharafHandler.routes(routes), settings)) { base =>
      val address = java.net.URI.create(base)
      val connection = new HttpConnection(new java.net.InetSocketAddress(address.getHost, address.getPort))
      try {
        val response = connection.request("OPTIONS", "/cors", headers = Seq("Origin" -> "http://example.com"))
        assertEquals(response.body, "ordinary options")
      } finally connection.close()
    }
  }

  test("preflight uses configured methods, headers and max age") {
    val routes = Routes { case OPTIONS -> Path("cors") => Response.withBody("ordinary options") }
    val settings = CorsSettings.default
      .withAllowedOrigins(Set("http://example.com"))
      .withAllowedHttpMethods(Set(HttpMethod.POST))
      .withAllowedHttpHeaders(Set(HttpString("X-Token")))
      .withPreflightMaxAge(Duration.ofSeconds(123))
    withHandler(SharafHandler.cors(SharafHandler.routes(routes), settings)) { base =>
      val address = java.net.URI.create(base)
      val connection = new HttpConnection(new java.net.InetSocketAddress(address.getHost, address.getPort))
      try {
        val response = connection.request(
          "OPTIONS",
          "/cors",
          headers = Seq(
            "Origin" -> "http://example.com",
            "Access-Control-Request-Method" -> "POST",
            "Access-Control-Request-Headers" -> "X-Token"
          )
        )
        assertEquals(response.headers.get("access-control-allow-origin"), Some("http://example.com"))
        assertEquals(response.headers.get("access-control-allow-methods"), Some("POST"))
        assertEquals(response.headers.get("access-control-allow-headers"), Some("X-Token"))
        assertEquals(response.headers.get("access-control-max-age"), Some("123"))
      } finally connection.close()
    }
  }

  for origin <- Seq(None, Some("http://example.com"), Some("http://other.com")) do {
    test(s"origin-dependent responses preserve Vary for origin $origin") {
      val routes = Routes { case GET -> Path("cors") =>
        Response.withBody("CORS").settingHeader("Vary", "Accept-Encoding")
      }
      val settings = CorsSettings.default.withAllowedOrigins(Set("http://example.com"))
      withHandler(SharafHandler.cors(SharafHandler.routes(routes), settings)) { base =>
        val response = request.get(uri"$base/cors")
          .headers(origin.map(value => HeaderNames.Origin -> value).toMap)
          .send(httpBackend)
        val vary = response.headers("Vary").flatMap(_.split(",")).map(_.trim.toLowerCase).toSet
        assertEquals(vary, Set("accept-encoding", "origin"))
      }
    }
  }

  test("existing wildcard Vary is preserved") {
    val routes = Routes { case GET -> Path("cors") => Response.withBody("CORS").settingHeader("Vary", "*") }
    val settings = CorsSettings.default.withAllowedOrigins(Set("http://example.com"))
    withHandler(SharafHandler.cors(SharafHandler.routes(routes), settings)) { base =>
      val response = request.get(uri"$base/cors").header("Origin", "http://example.com").send(httpBackend)
      assertEquals(response.headers("Vary"), Seq("*"))
    }
  }

  test("wildcard origin with credentials does not reflect an arbitrary origin") {
    val routes = Routes { case GET -> Path("cors") => Response.withBody("CORS") }
    val settings = CorsSettings.default.withAllowedOrigins(Set("*")).withAllowCredentials(true)
    withHandler(SharafHandler.cors(SharafHandler.routes(routes), settings)) { base =>
      val response = request.get(uri"$base/cors").header("Origin", "http://other.com").send(httpBackend)
      assertEquals(response.header("Access-Control-Allow-Origin"), Some("*"))
      assertEquals(response.header("Access-Control-Allow-Credentials"), Some("true"))
    }
  }
}
