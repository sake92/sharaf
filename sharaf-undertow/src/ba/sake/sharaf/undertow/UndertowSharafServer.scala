package ba.sake.sharaf.undertow

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.handlers.resource.ResourceHandler
import io.undertow.server.handlers.resource.ClassPathResourceManager
import ba.sake.sharaf.*

class UndertowSharafServer(host: String, port: Int, handler: SharafUndertowHandler) {

  private val finalHandler = BlockingHandler(handler)
  private val server = Undertow
    .builder()
    .addHttpListener(port, host)
    .setHandler(finalHandler)
    .build()

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}

object UndertowSharafServer {

  def apply(host: String, port: Int, sharafHandler: SharafHandler): UndertowSharafServer =
    new UndertowSharafServer(host, port, SharafUndertowHandler(sharafHandler))

  def apply(
      host: String,
      port: Int,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): UndertowSharafServer = {
    // TODO manually implement serving static files from public/ folder and webjars
    val resourceHandler = ResourceHandler( // load from classpath in public/ folder
      ClassPathResourceManager(getClass.getClassLoader, "public"), {
        // or load from classpath in WebJars
        val webJarHandler = new ResourceHandler(
          ClassPathResourceManager(getClass.getClassLoader, "META-INF/resources/webjars"),
          SharafUndertowHandler(notFoundHandler) // handle 404s at the end
        )
        // dont serve index.html etc from random webjars...
        webJarHandler.setWelcomeFiles()
        webJarHandler
      }
    )
    val finalHandler = SharafUndertowHandler(
      SharafHandler.exceptions(
        exceptionMapper,
        SharafHandler.cors(
          corsSettings,
          SharafHandler.routes(routes, notFoundHandler)
        )
      ),
      notFoundHandler = Some(resourceHandler)
    )
    new UndertowSharafServer(host, port, finalHandler)
  }

}
