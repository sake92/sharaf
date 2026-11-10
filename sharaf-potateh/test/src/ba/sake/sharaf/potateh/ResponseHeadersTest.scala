package ba.sake.sharaf.potateh

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractResponseHeadersTest
import ba.sake.potateh.ServerConfig

class ResponseHeadersTest extends AbstractResponseHeadersTest {
  override def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit = {
    val server = PotatehSharafServer.start(
      ServerConfig.Default.withBindAddress(new java.net.InetSocketAddress("127.0.0.1", 0)),
      routes,
      exceptionMapper = mapper
    )
    try check(s"http://127.0.0.1:${server.address.getPort}")
    finally server.stop()
  }
}
