package ba.sake.sharaf.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.handlers.resource.ResourceHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import io.undertow.util.StatusCodes

import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.ExceptionMapper
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.undertow.*

final class SharafHandler private (
    routes: UndertowSharafRoutes,
    corsSettings: CorsSettings,
    exceptionMapper: ExceptionMapper,
    notFoundHandler: Request => Response[?]
) extends HttpHandler {

  private val notFoundRoutes = UndertowSharafRoutes { _ =>
    notFoundHandler(Request.current)
  }

  // everything is wrapped in a synchronous/blocking handler
  private val finalHandler =
    BlockingHandler( // synchronous/blocking handler
      ExceptionHandler( // handle exceptions gracefully
        CorsHandler( // handle CORS preflight requests
          RoutesHandler( // main Sharaf routes handler
            routes,
            ResourceHandler( // or else load from classpath in public/ folder
              ClassPathResourceManager(getClass.getClassLoader, "public"), {
                // or else load from classpath in WebJars
                val webJarHandler = new ResourceHandler(
                  ClassPathResourceManager(getClass.getClassLoader, "META-INF/resources/webjars"),
                  RoutesHandler(notFoundRoutes) // handle 404s at the end
                )
                // dont serve index.html etc from random webjars...
                webJarHandler.setWelcomeFiles()
                webJarHandler
              }
            )
          ),
          corsSettings
        ),
        exceptionMapper
      )
    )

  override def handleRequest(exchange: HttpServerExchange): Unit =
    finalHandler.handleRequest(exchange)

  def withRoutes(routes: UndertowSharafRoutes): SharafHandler =
    copy(routes)

  def withCorsSettings(corsSettings: CorsSettings): SharafHandler =
    copy(corsSettings = corsSettings)

  def withExceptionMapper(exceptionMapper: ExceptionMapper): SharafHandler =
    copy(exceptionMapper = exceptionMapper)

  def withNotFoundHandler(notFoundHandler: Request => Response[?]): SharafHandler =
    copy(notFoundHandler = notFoundHandler)

  private def copy(
      routes: UndertowSharafRoutes = routes,
      corsSettings: CorsSettings = corsSettings,
      exceptionMapper: ExceptionMapper = exceptionMapper,
      notFoundHandler: Request => Response[?] = notFoundHandler
  ) = new SharafHandler(routes, corsSettings, exceptionMapper, notFoundHandler)
}

// TODO rename to SharafUndertowHandler
object SharafHandler:

  private val defaultNotFoundResponse = Response.withBody("Not Found").withStatus(StatusCodes.NOT_FOUND)

  def apply(routes: UndertowSharafRoutes): SharafHandler =
    new SharafHandler(routes, CorsSettings.default, ExceptionMapper.default, _ => SharafHandler.defaultNotFoundResponse)
    
  def apply(controllers: SharafController[UndertowSharafRequest]*): SharafHandler =
    val routes = SharafRoutes.merge(controllers.map(_.routes))
    apply(routes)
