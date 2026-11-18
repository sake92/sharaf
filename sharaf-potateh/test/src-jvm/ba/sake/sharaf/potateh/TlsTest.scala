package ba.sake.sharaf.potateh

import scala.util.chaining.*
import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.*

class TlsTest extends munit.FunSuite {
  test("shared TLS configuration serves HTTPS") {
    val tls = TlsTestSupport.config()
    val config = ServerConfig.Default.withBindAddress(new InetSocketAddress("127.0.0.1", 0))
    val routes = Routes { case GET -> Path() => Response.withBody("secure") }
    val server = PotatehSharafServer(routes, "127.0.0.1", 0).withConfig(PotatehTls.configure(config, tls)).tap(_.start())
    try assert(TlsTestSupport.get(tls, server.port).endsWith("secure"))
    finally server.stop()
  }
}
