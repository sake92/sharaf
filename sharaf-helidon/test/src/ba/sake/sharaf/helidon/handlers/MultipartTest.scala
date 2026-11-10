package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractMultipartTest
import ba.sake.sharaf.helidon.HelidonSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class MultipartTest extends AbstractMultipartTest {
  override def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit = {
    val port = NetworkUtils.getFreePort()
    val server = HelidonSharafServer("127.0.0.1", port, routes, exceptionMapper = mapper)
    server.start()
    try check(s"http://127.0.0.1:$port")
    finally server.stop()
  }
}
