package ba.sake.sharaf.potateh

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.ServerFixture
import ba.sake.potateh.ServerConfig

trait PotatehServerFixture extends ServerFixture {
  override def withDefaultRoutes(routes: Routes)(check: String => Unit): Unit = {
    val server = PotatehSharafServer.start(
      ServerConfig.Default.withBindAddress(new java.net.InetSocketAddress("127.0.0.1", 0)),
      routes
    )
    try check(s"http://127.0.0.1:${server.address.getPort}")
    finally server.stop()
  }

  override def withHandler(handler: SharafHandler)(check: String => Unit): Unit = {
    val server = PotatehSharafServer.start(
      ServerConfig.Default.withBindAddress(new java.net.InetSocketAddress("127.0.0.1", 0)),
      handler
    )
    try check(s"http://127.0.0.1:${server.address.getPort}")
    finally server.stop()
  }
}
