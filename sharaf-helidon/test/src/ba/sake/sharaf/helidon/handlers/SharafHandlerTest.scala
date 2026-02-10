package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractSharafHandlerTest
import ba.sake.sharaf.helidon.HelidonSharafServer

class SharafHandlerTest extends AbstractSharafHandlerTest {
  val server = HelidonSharafServer("localhost", port, routes)
  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}