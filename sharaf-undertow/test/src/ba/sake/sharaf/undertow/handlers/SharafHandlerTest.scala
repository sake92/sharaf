package ba.sake.sharaf.undertow.handlers

import ba.sake.sharaf.handlers.AbstractSharafHandlerTest
import ba.sake.sharaf.undertow.*

class SharafHandlerTest extends AbstractSharafHandlerTest {
  val server = UndertowSharafServer("localhost", port, routes)

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
