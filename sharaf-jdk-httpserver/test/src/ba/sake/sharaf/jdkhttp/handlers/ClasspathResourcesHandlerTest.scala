package ba.sake.sharaf.jdkhttp.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractClasspathResourcesHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

class ClasspathResourcesHandlerTest extends AbstractClasspathResourcesHandlerTest {
  val server =
    JdkHttpServerSharafServer("localhost", port, SharafHandler.exceptions(classpathResourcesHandler, ExceptionMapper.default))
  override def startServer(): Unit = server.start()
  override def stopServer(): Unit = server.stop()
}
