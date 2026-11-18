package ba.sake.sharaf.undertow

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.ServerFixture
import ba.sake.sharaf.undertow.UndertowSharafServer

trait UndertowServerFixture extends ServerFixture {
  override def withDefaultRoutes(routes: Routes)(check: String => Unit): Unit = {
    val server = UndertowSharafServer(routes)
    server.start()
    try check(s"http://127.0.0.1:${server.port}")
    finally server.stop()
  }

  override def withHandler(handler: SharafHandler)(check: String => Unit): Unit = {
    val server = UndertowSharafServer(handler, "127.0.0.1", 0, None)
    server.start()
    try check(s"http://127.0.0.1:${server.port}")
    finally server.stop()
  }
}
