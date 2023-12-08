package ba.sake.sharaf.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.ResourceHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import ba.sake.sharaf.routing.Routes
import ba.sake.sharaf.Request
import ba.sake.sharaf.Response
import io.undertow.util.StatusCodes

class SharafHandler(
    routes: Routes,
    corsSettings: CorsSettings = CorsSettings(),
    errorMapper: ErrorMapper = ErrorMapper.default,
    notFoundHandler: Request => Response[?] = _ => SharafHandler.defaultNotFoundResponse
) extends HttpHandler {

  private val notFoundRoutes = Routes { case _ =>
    notFoundHandler(Request.current)
  }

  private val finalHandler = ErrorHandler(
    CorsHandler(
      RoutesHandler(
        routes,
        ResourceHandler(
          ClassPathResourceManager(getClass.getClassLoader, "public"),
          RoutesHandler(notFoundRoutes) // handle 404s at the end
        )
      ),
      corsSettings
    ),
    errorMapper
  )

  def withRoutes(routes: Routes): SharafHandler =
    new SharafHandler(routes, corsSettings, errorMapper)

  def withCorsSettings(corsSettings: CorsSettings): SharafHandler =
    new SharafHandler(routes, corsSettings, errorMapper)

  def withErrorMapper(errorMapper: ErrorMapper): SharafHandler =
    new SharafHandler(routes, corsSettings, errorMapper)

  def withNotFoundHandler(notFoundHandler: Request => Response[?]): SharafHandler =
    new SharafHandler(routes, corsSettings, errorMapper, notFoundHandler)

  override def handleRequest(exchange: HttpServerExchange): Unit =
    finalHandler.handleRequest(exchange)
}

object SharafHandler:

  private[sharaf] val defaultNotFoundResponse = Response.withBody("Not Found").withStatus(StatusCodes.NOT_FOUND)

  def apply(routes: Routes): SharafHandler =
    new SharafHandler(routes, CorsSettings(), ErrorMapper.default)
