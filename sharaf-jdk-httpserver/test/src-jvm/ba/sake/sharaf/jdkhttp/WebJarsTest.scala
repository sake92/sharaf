package ba.sake.sharaf.jdkhttp

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractWebJarsTest

class WebJarsTest extends AbstractWebJarsTest {
  override def withRoutesServer(routes: Routes)(check: String => Unit): Unit = {
    val server = JdkHttpServerSharafServer("127.0.0.1", routes)
    server.start()
    try check(s"http://127.0.0.1:${server.port}")
    finally server.stop()
  }
}
