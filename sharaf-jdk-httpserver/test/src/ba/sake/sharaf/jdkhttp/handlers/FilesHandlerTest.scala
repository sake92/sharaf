package ba.sake.sharaf.jdkhttp.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractFilesHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class FilesHandlerTest extends AbstractFilesHandlerTest {

  val port = NetworkUtils.getFreePort()
  val server = JdkHttpServerSharafServer("localhost", port, SharafHandler.exceptions(routesHandler, ExceptionMapper.default))

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
