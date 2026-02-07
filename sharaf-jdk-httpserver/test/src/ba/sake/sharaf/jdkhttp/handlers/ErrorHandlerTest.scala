package ba.sake.sharaf.jdkhttp.handlers

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.sun.net.httpserver.HttpServer
import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractErrorHandlerTest
import ba.sake.sharaf.jdkhttp.SharafJdkHttpHandler
import ba.sake.sharaf.utils.NetworkUtils

class ErrorHandlerTest extends AbstractErrorHandlerTest {

  val port = NetworkUtils.getFreePort()

  private val httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 0)
  
  // Create handlers with different exception mappers
  val defaultHandler = SharafHandler.exceptions(SharafHandler.routes(routes))
  val jsonHandler = SharafHandler.exceptions(SharafHandler.routes(routes), ExceptionMapper.json)
  
  // Register handlers at different contexts
  httpServer.createContext("/default", SharafJdkHttpHandler(defaultHandler))
  httpServer.createContext("/json", SharafJdkHttpHandler(jsonHandler))
  httpServer.setExecutor(Executors.newFixedThreadPool(10))

  def startServer(): Unit = httpServer.start()
  def stopServer(): Unit = httpServer.stop(0)
}
