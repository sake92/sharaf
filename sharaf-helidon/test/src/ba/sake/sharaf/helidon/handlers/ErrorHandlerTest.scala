package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.AbstractErrorHandlerTest
import ba.sake.sharaf.helidon.HelidonSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class ErrorHandlerTest extends AbstractErrorHandlerTest {

  val port = NetworkUtils.getFreePort()

  // Create handlers with different exception mappers
  val defaultHandler = SharafHandler.exceptions(SharafHandler.routes(routes))
  val jsonHandler = SharafHandler.exceptions(SharafHandler.routes(routes), ExceptionMapper.json)

  // Need a custom handler that routes based on prefix and strips it
  val combinedHandler = new SharafHandler {
    override def handle(requestContext: RequestContext): Response = {
      val (method, path) = requestContext.params
      path.segments.headOption match {
        case Some("default") =>
          val strippedContext = requestContext.copy(params = (method, Path(path.segments.tail*)))
          defaultHandler.handle(strippedContext)
        case Some("json") =>
          val strippedContext = requestContext.copy(params = (method, Path(path.segments.tail*)))
          jsonHandler.handle(strippedContext)
        case _ =>
          Response.notFound
      }
    }
  }

  val server = HelidonSharafServer("localhost", port, combinedHandler)

  def startServer(): Unit = server.start()
  def stopServer(): Unit = server.stop()
}

