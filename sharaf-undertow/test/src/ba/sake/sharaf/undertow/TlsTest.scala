package ba.sake.sharaf.undertow

import ba.sake.sharaf.*

class TlsTest extends munit.FunSuite {
  test("shared TLS configuration serves HTTPS") {
    val tls = TlsTestSupport.config()
    val routes = Routes { case GET -> Path() => Response.withBody("secure") }
    val server = UndertowSharafServer(routes, tls = Some(tls))
    server.start()
    try assert(TlsTestSupport.get(tls, server.port).endsWith("secure"))
    finally server.stop()
  }
}
