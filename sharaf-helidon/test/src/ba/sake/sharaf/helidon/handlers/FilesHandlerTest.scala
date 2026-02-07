package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractFilesHandlerTest
import ba.sake.sharaf.helidon.HelidonSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class FilesHandlerTest extends AbstractFilesHandlerTest {

  val port = NetworkUtils.getFreePort()
  val server = HelidonSharafServer("localhost", port, SharafHandler.exceptions(routesHandler, ExceptionMapper.default))

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}