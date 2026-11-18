package ba.sake.sharaf.potateh

import scala.util.chaining.*
import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.ServerFixture

trait PotatehServerFixture extends ServerFixture {
  override def withDefaultRoutes(routes: Routes)(check: String => Unit): Unit = {
    val server = PotatehSharafServer("127.0.0.1", 0, routes).tap(_.start())
    try check(s"http://127.0.0.1:${server.port}")
    finally server.stop()
  }

  override def withHandler(handler: SharafHandler)(check: String => Unit): Unit = {
    val server = PotatehSharafServer("127.0.0.1", 0, handler).tap(_.start())
    try check(s"http://127.0.0.1:${server.port}")
    finally server.stop()
  }
}
