package ba.sake.sharaf.undertow

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.ServerFixture
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.sharaf.utils.NetworkUtils

trait UndertowServerFixture extends ServerFixture {
  override def withHandler(handler: SharafHandler)(check: String => Unit): Unit = {
    val port = NetworkUtils.getFreePort()
    val server = UndertowSharafServer("127.0.0.1", port, handler)
    server.start()
    try check(s"http://127.0.0.1:$port")
    finally server.stop()
  }
}
