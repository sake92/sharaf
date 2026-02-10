package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractErrorHandlerTest
import ba.sake.sharaf.helidon.HelidonSharafServer
import sttp.model.StatusCode

class ErrorHandlerTest extends AbstractErrorHandlerTest {
  val defaultHandler = SharafHandler.exceptions(SharafHandler.routes(routes))
  val jsonHandler = SharafHandler.exceptions(SharafHandler.routes(routes), ExceptionMapper.json)

  // custom handler that routes based on prefix
  val combinedHandler = new SharafHandler {
    override def handle(requestContext: RequestContext): Response[?] = {
      val (method, path) = requestContext.params
      path.segments.headOption match {
        case Some("default") =>
          val strippedContext = requestContext.copy(params = (method, Path(path.segments.tail*)))
          defaultHandler.handle(strippedContext)
        case Some("json") =>
          val strippedContext = requestContext.copy(params = (method, Path(path.segments.tail*)))
          jsonHandler.handle(strippedContext)
        case _ =>
          Response.withStatus(StatusCode.NotFound).withBody("Not found")
      }
    }
  }

  val server = HelidonSharafServer("localhost", port, combinedHandler)

  override def startServer(): Unit = server.start()
  override def stopServer(): Unit = server.stop()

  override def supportsForms: Boolean = false // TODO
}
