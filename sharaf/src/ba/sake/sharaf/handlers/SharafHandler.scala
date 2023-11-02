package ba.sake.sharaf.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.ResourceHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import ba.sake.sharaf.routing.Routes

class SharafHandler(
    routes: Routes,
    corsSettings: CorsSettings = CorsSettings(),
    errorMapper: ErrorMapper = ErrorMapper.default
) extends HttpHandler {
  private val finalHandler = ErrorHandler(
    CorsHandler(
      RoutesHandler(
        routes,
        ResourceHandler(ClassPathResourceManager(getClass.getClassLoader, "static"))
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

  override def handleRequest(exchange: HttpServerExchange): Unit =
    finalHandler.handleRequest(exchange)
}

object SharafHandler:
  def apply(routes: Routes): SharafHandler =
    new SharafHandler(routes, CorsSettings(), ErrorMapper.default)
