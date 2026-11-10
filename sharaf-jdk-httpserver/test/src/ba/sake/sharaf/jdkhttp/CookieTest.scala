package ba.sake.sharaf.jdkhttp

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractCookieTest
import ba.sake.sharaf.utils.NetworkUtils

class CookieTest extends AbstractCookieTest {
  override def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit = {
    val port = NetworkUtils.getFreePort()
    val handler = SharafHandler.exceptions(SharafHandler.routes(routes), mapper)
    val server = JdkHttpServerSharafServer("127.0.0.1", port, handler)
    server.start()
    try check(s"http://127.0.0.1:$port")
    finally server.stop()
  }
}
