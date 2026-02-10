package ba.sake.sharaf.jdkhttp.handlers

import ba.sake.sharaf.handlers.AbstractSharafHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

class SharafHandlerTest extends AbstractSharafHandlerTest {

  val server = JdkHttpServerSharafServer("localhost", port, routes)

  override def startServer(): Unit = server.start()
  override def stopServer(): Unit = server.stop()
}