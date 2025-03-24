package ba.sake.sharaf.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.ResourceHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import io.undertow.util.StatusCodes
import ba.sake.sharaf.routing.Routes
import ba.sake.sharaf.Request
import ba.sake.sharaf.Response
import ba.sake.sharaf.exceptions.ExceptionMapper
import ba.sake.sharaf.handlers.cors.*

final class SharafHandler private (
    routes: Routes,
    corsSettings: CorsSettings,
    exceptionMapper: ExceptionMapper,
    notFoundHandler: Request => Response[?]
) extends HttpHandler {

  private val notFoundRoutes = Routes { case _ =>
    notFoundHandler(Request.current)
  }

  private val finalHandler = ExceptionHandler(
    CorsHandler(
      RoutesHandler(
        routes,
        ResourceHandler(
          ClassPathResourceManager(getClass.getClassLoader, "public"),
          ResourceHandler(
            ClassPathResourceManager(getClass.getClassLoader, "META-INF/resources/webjars"),
            RoutesHandler(notFoundRoutes) // handle 404s at the end
          )
        )
      ),
      corsSettings
    ),
    exceptionMapper
  )

  override def handleRequest(exchange: HttpServerExchange): Unit =
    finalHandler.handleRequest(exchange)

  def withRoutes(routes: Routes): SharafHandler =
    copy(routes)

  def withCorsSettings(corsSettings: CorsSettings): SharafHandler =
    copy(corsSettings = corsSettings)

  def withExceptionMapper(exceptionMapper: ExceptionMapper): SharafHandler =
    copy(exceptionMapper = exceptionMapper)

  def withNotFoundHandler(notFoundHandler: Request => Response[?]): SharafHandler =
    copy(notFoundHandler = notFoundHandler)

  private def copy(
      routes: Routes = routes,
      corsSettings: CorsSettings = corsSettings,
      exceptionMapper: ExceptionMapper = exceptionMapper,
      notFoundHandler: Request => Response[?] = notFoundHandler
  ) = new SharafHandler(routes, corsSettings, exceptionMapper, notFoundHandler)
}

object SharafHandler:

  private val defaultNotFoundResponse = Response.withBody("Not Found").withStatus(StatusCodes.NOT_FOUND)

  def apply(routes: Routes): SharafHandler =
    new SharafHandler(routes, CorsSettings.default, ExceptionMapper.default, _ => SharafHandler.defaultNotFoundResponse)
