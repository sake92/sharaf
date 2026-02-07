package ba.sake.sharaf.jdkhttp.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractSharafHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class SharafHandlerTest extends AbstractSharafHandlerTest {

  val port = NetworkUtils.getFreePort()
  val server = JdkHttpServerSharafServer("localhost", port, routes)

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}
