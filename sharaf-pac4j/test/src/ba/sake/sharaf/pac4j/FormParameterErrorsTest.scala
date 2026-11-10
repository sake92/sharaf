package ba.sake.sharaf.pac4j

import java.net.InetSocketAddress
import sttp.client4.quick.*
import sttp.model.{HttpVersion, StatusCode}
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer
import ba.sake.sharaf.undertow.UndertowSharafServer

class FormParameterErrorsTest extends munit.FunSuite {
  private val request = quickRequest.httpVersion(HttpVersion.HTTP_1_1)
  private val routes = Routes {
    case POST -> Path("parameter") =>
      val context = SharafWebContext(Request.current, "http://localhost/parameter", HttpMethod.POST)
      Response.withBody(context.getRequestParameter("token").orElse("missing"))
  }
  private val handler = SharafHandler.exceptions(SharafHandler.routes(routes))

  private def withServer(adapter: String)(check: String => Unit): Unit =
    if adapter == "undertow" then {
      val server = UndertowSharafServer("127.0.0.1", handler)
      server.start()
      try check(s"http://127.0.0.1:${server.port}")
      finally server.stop()
    } else {
      val config = ServerConfig.Default.withBindAddress(InetSocketAddress("127.0.0.1", 0))
      val server = PotatehSharafServer.start(config, handler)
      try check(s"http://127.0.0.1:${server.address.getPort}")
      finally server.stop()
    }

  for adapter <- Seq("undertow", "potateh") do {
    test(s"$adapter form errors do not fall back to query parameters") {
      withServer(adapter) { base =>
        val malformed = request.post(uri"$base/parameter?token=query")
          .contentType("application/x-www-form-urlencoded")
          .body("token=%XX").send()
        assertEquals(malformed.code, StatusCode.BadRequest)

        val oversized = request.post(uri"$base/parameter?token=query")
          .contentType("application/x-www-form-urlencoded")
          .body("token=" + "a" * (1024 * 1024)).send()
        assertEquals(oversized.code, StatusCode.PayloadTooLarge)
      }
    }

    test(s"$adapter form parameters override query and non-form requests use query") {
      withServer(adapter) { base =>
        val form = request.post(uri"$base/parameter?token=query")
          .contentType("application/x-www-form-urlencoded")
          .body("token=form").send()
        assertEquals(form.body, "form")
        val other = request.post(uri"$base/parameter?token=query")
          .contentType("text/plain").body("ignored").send()
        assertEquals(other.body, "query")
      }
    }
  }
}
