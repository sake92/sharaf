package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractFilesHandlerTest
import ba.sake.sharaf.helidon.HelidonSharafServer

class FilesHandlerTest extends AbstractFilesHandlerTest {
  // this can't use just routes
  // because we serve static files, which are not handled by default, just the classpath..
  val server = HelidonSharafServer("localhost", port, SharafHandler.exceptions(routesHandler, ExceptionMapper.default))
  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}