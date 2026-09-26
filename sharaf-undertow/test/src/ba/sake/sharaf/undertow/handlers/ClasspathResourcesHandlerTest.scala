package ba.sake.sharaf.undertow.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractClasspathResourcesHandlerTest
import ba.sake.sharaf.undertow.UndertowSharafServer

class ClasspathResourcesHandlerTest extends AbstractClasspathResourcesHandlerTest {
  val server =
    UndertowSharafServer("localhost", port, SharafHandler.exceptions(classpathResourcesHandler, ExceptionMapper.default))
  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
