package ba.sake.sharaf.undertow.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractResponseHeadersTest
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class ResponseHeadersTest extends AbstractResponseHeadersTest {
  override def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit = {
    val port = NetworkUtils.getFreePort()
    val server = UndertowSharafServer("127.0.0.1", port, routes, exceptionMapper = mapper)
    server.start()
    try check(s"http://127.0.0.1:$port")
    finally server.stop()
  }
}
