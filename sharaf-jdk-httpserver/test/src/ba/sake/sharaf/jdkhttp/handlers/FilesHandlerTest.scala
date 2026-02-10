package ba.sake.sharaf.jdkhttp.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractFilesHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

class FilesHandlerTest extends AbstractFilesHandlerTest {

  val server =
    JdkHttpServerSharafServer("localhost", port, SharafHandler.exceptions(routesHandler, ExceptionMapper.default))

  override def startServer(): Unit = server.start()
  override def stopServer(): Unit = server.stop()
}
