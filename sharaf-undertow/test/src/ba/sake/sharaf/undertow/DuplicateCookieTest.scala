package ba.sake.sharaf.undertow

import java.net.InetSocketAddress
import io.undertow.{Undertow, UndertowOptions}
import io.undertow.server.handlers.BlockingHandler
import sttp.client4.quick.*
import sttp.model.StatusCode
import ba.sake.sharaf.*

class DuplicateCookieTest extends ba.sake.sharaf.handlers.AbstractDuplicateCookieTest with UndertowServerFixture {
  test("Undertow honors its configured cookie limit") {
    val routes = Routes {
      case GET -> Path("cookies") => Response.withBody(Request.current.cookies.size.toString)
    }
    val handler = SharafHandler.exceptions(SharafHandler.routes(routes))
    val server = Undertow.builder()
      .addHttpListener(0, "127.0.0.1")
      .setServerOption(UndertowOptions.MAX_COOKIES, 2)
      .setHandler(BlockingHandler(SharafUndertowHandler(handler)))
      .build()
    server.start()
    try {
      val port = server.getListenerInfo.get(0).getAddress.asInstanceOf[InetSocketAddress].getPort
      val base = s"http://127.0.0.1:$port"
      val allowed = request.get(uri"$base/cookies").header("Cookie", "a=1; b=2").send(httpBackend)
      assertEquals(allowed.body, "2")
      val excessive = request.get(uri"$base/cookies").header("Cookie", "a=1; b=2; c=3").send(httpBackend)
      assertEquals(excessive.code, StatusCode.BadRequest)
    } finally server.stop()
  }
}
