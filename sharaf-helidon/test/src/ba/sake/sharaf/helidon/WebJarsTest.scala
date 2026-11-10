package ba.sake.sharaf.helidon

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractWebJarsTest
import ba.sake.sharaf.utils.NetworkUtils

class WebJarsTest extends AbstractWebJarsTest {
  override def withRoutesServer(routes: Routes)(check: String => Unit): Unit = {
    val port = NetworkUtils.getFreePort()
    val server = HelidonSharafServer("127.0.0.1", port, routes)
    server.start()
    try check(s"http://127.0.0.1:$port")
    finally server.stop()
  }
}
