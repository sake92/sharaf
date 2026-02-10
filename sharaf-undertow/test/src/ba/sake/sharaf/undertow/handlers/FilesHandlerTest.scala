package ba.sake.sharaf.undertow.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractFilesHandlerTest
import ba.sake.sharaf.undertow.UndertowSharafServer


class FilesHandlerTest extends AbstractFilesHandlerTest {

  val server = UndertowSharafServer("localhost", port, SharafHandler.exceptions(routesHandler, ExceptionMapper.default))

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()

}
