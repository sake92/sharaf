package ba.sake.sharaf.potateh

import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.*

class TlsTest extends munit.FunSuite {
  test("shared TLS configuration serves HTTPS") {
    val tls = TlsTestSupport.config()
    val config = ServerConfig.Default.withBindAddress(new InetSocketAddress("127.0.0.1", 0))
    val routes = Routes { case GET -> Path() => Response.withBody("secure") }
    val server = PotatehSharafServer.start(PotatehTls.configure(config, tls), routes)
    try assert(TlsTestSupport.get(tls, server.address.getPort).endsWith("secure"))
    finally server.stop()
  }
}
