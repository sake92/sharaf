package ba.sake.sharaf.helidon.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.AbstractErrorHandlerTest
import ba.sake.sharaf.helidon.HelidonSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class ErrorHandlerTest extends AbstractErrorHandlerTest {

  val port = NetworkUtils.getFreePort()

  // Create routes with prefixes included
  val routesWithPrefixes = Routes {
    case GET -> Path("default", "query") =>
      val qp = Request.current.queryParamsValidated[TestQuery]
      Response.withBody(qp.toString)
    case POST -> Path("default", "form") =>
      val body = Request.current.bodyFormValidated[TestForm]
      Response.withBody(body.toString)
    case POST -> Path("default", "json") =>
      val body = Request.current.bodyJsonValidated[TestJson]
      Response.withBody(body)
    case GET -> Path("default") =>
      Response.withBody("OK")
    case GET -> Path("json", "query") =>
      val qp = Request.current.queryParamsValidated[TestQuery]
      Response.withBody(qp.toString)
    case POST -> Path("json", "form") =>
      val body = Request.current.bodyFormValidated[TestForm]
      Response.withBody(body.toString)
    case POST -> Path("json", "json") =>
      val body = Request.current.bodyJsonValidated[TestJson]
      Response.withBody(body)
    case GET -> Path("json") =>
      Response.withBody("OK")
  }

  val handler = SharafHandler.routes(routesWithPrefixes)
  val defaultHandler = SharafHandler.exceptions(handler)
  val jsonHandler = SharafHandler.exceptions(handler, ExceptionMapper.json)

  // Need a custom handler that routes based on prefix
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

