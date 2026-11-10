package ba.sake.sharaf.jdkhttp

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.ServerFixture

trait JdkHttpServerServerFixture extends ServerFixture {
  override def withHandler(handler: SharafHandler)(check: String => Unit): Unit = {
    val server = JdkHttpServerSharafServer("127.0.0.1", handler)
    server.start()
    try check(s"http://127.0.0.1:${server.port}")
    finally server.stop()
  }
}
