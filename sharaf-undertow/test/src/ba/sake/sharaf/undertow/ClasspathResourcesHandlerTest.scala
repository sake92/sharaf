package ba.sake.sharaf.undertow

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractClasspathResourcesHandlerTest
import ba.sake.sharaf.undertow.UndertowSharafServer

class ClasspathResourcesHandlerTest extends AbstractClasspathResourcesHandlerTest {
  val server =
    UndertowSharafServer(
      "localhost",
      SharafHandler.exceptions(classpathResourcesHandler, ExceptionMapper.default)
    )
  def port: Int = server.port
  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
