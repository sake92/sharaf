package ba.sake.sharaf.undertow

import io.undertow.Undertow
import ba.sake.sharaf.*
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.handlers.resource.ResourceHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import io.undertow.server.HttpHandler
import sttp.model.StatusCode

class UndertowSharafServer(host: String, port: Int, handler: HttpHandler) {

  private val server = Undertow
    .builder()
    .addHttpListener(port, host)
    .setHandler(handler)
    .build()

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}

object UndertowSharafServer {

  private val defaultNotFoundResponse = Response.withBody("Not Found").withStatus(StatusCode.NotFound)

  def apply(
      host: String,
      port: Int,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: Request => Response[?] = _ => defaultNotFoundResponse
  ): UndertowSharafServer = {
    val notFoundRoutes = Routes { _ =>
      notFoundHandler(Request.current)
    }
    val resourceHandler = ResourceHandler( // load from classpath in public/ folder
      ClassPathResourceManager(getClass.getClassLoader, "public"), {
        // or load from classpath in WebJars
        val webJarHandler = new ResourceHandler(
          ClassPathResourceManager(getClass.getClassLoader, "META-INF/resources/webjars"),
          SharafUndertowHandler(SharafHandler.routes(notFoundRoutes)) // handle 404s at the end
        )
        // dont serve index.html etc from random webjars...
        webJarHandler.setWelcomeFiles()
        webJarHandler
      }
    )
    val finalHandler =
      BlockingHandler( // synchronous/blocking handler
        UndertowExceptionHandler(
          exceptionMapper,
          next = SharafUndertowHandler(
            SharafHandler.cors(
              corsSettings,
              SharafHandler.routes(routes)
            ),
            next = Some(resourceHandler)
          )
        )
      )

    new UndertowSharafServer(host, port, finalHandler)
  }

  def apply(host: String, port: Int, sharafHandler: SharafHandler): UndertowSharafServer =
    new UndertowSharafServer(host, port, SharafUndertowHandler(sharafHandler))

  // if need tweaking
  def apply(host: String, port: Int, sharafUndertowHandler: SharafUndertowHandler): UndertowSharafServer =
    new UndertowSharafServer(host, port, sharafUndertowHandler)
}
