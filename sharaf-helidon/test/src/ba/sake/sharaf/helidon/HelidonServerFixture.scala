package ba.sake.sharaf.helidon

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.ServerFixture
import ba.sake.sharaf.helidon.HelidonSharafServer

trait HelidonServerFixture extends ServerFixture {
  override def withHandler(handler: SharafHandler)(check: String => Unit): Unit = {
    val server = HelidonSharafServer("127.0.0.1", handler)
    server.start()
    try check(s"http://127.0.0.1:${server.port}")
    finally server.stop()
  }
}
