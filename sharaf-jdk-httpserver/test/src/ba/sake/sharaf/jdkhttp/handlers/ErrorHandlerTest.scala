package ba.sake.sharaf.jdkhttp.handlers

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.AbstractErrorHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafRequest

class ErrorHandlerTest extends AbstractErrorHandlerTest {

  private val httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 0)
  
  // Create handlers with different exception mappers
  val defaultHandler = SharafHandler.exceptions(SharafHandler.routes(routes))
  val jsonHandler = SharafHandler.exceptions(SharafHandler.routes(routes), ExceptionMapper.json)
  
  // Custom handler that strips the prefix
  class PrefixStrippingHandler(prefix: String, handler: SharafHandler) extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val reqParams = fillReqParams(exchange, prefix)
      val req = JdkHttpServerSharafRequest.create(exchange)
      val requestContext = RequestContext(reqParams, req)
      val res = handler.handle(requestContext)
      ba.sake.sharaf.jdkhttp.ResponseUtils.writeResponse(res, exchange)
    }

    private def fillReqParams(exchange: HttpExchange, prefix: String): RequestParams = {
      val method = HttpMethod.valueOf(exchange.getRequestMethod)
      val fullPath = exchange.getRequestURI.getPath
      val pathAfterPrefix = if (fullPath.startsWith(prefix)) fullPath.drop(prefix.length) else fullPath
      val relPath = if (pathAfterPrefix.startsWith("/")) pathAfterPrefix.drop(1) else pathAfterPrefix
      val pathSegments = relPath.split("/")
      val path =
        if (pathSegments.size == 1 && pathSegments.head == "")
        then Path()
        else Path(pathSegments*)
      (method, path)
    }
  }
  
  // Register handlers at different contexts
  httpServer.createContext("/default", PrefixStrippingHandler("/default", defaultHandler))
  httpServer.createContext("/json", PrefixStrippingHandler("/json", jsonHandler))
  httpServer.setExecutor(Executors.newFixedThreadPool(10))

  override def startServer(): Unit = httpServer.start()
  override def stopServer(): Unit = httpServer.stop(0)

  override def supportsForms: Boolean = false // TODO
}