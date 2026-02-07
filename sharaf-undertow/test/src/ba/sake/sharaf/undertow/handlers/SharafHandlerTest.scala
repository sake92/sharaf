package ba.sake.sharaf.undertow.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractSharafHandlerTest
import ba.sake.sharaf.undertow.*
import ba.sake.sharaf.utils.NetworkUtils

class SharafHandlerTest extends AbstractSharafHandlerTest {

  val port = NetworkUtils.getFreePort()

  val server = UndertowSharafServer("localhost", port, routes)

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
