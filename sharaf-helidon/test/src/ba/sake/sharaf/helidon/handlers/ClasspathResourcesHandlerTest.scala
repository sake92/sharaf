package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractClasspathResourcesHandlerTest
import ba.sake.sharaf.helidon.HelidonSharafServer

class ClasspathResourcesHandlerTest extends AbstractClasspathResourcesHandlerTest {
  val server =
    HelidonSharafServer("localhost", port, SharafHandler.exceptions(classpathResourcesHandler, ExceptionMapper.default))
  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
