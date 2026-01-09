package ba.sake.sharaf.undertow

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
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
    val cpResHandler = SharafHandler.classpathResources(
      "public",
      SharafHandler.classpathResources("META-INF/resources/webjars", notFoundHandler)
    )
    val finalHandler = SharafUndertowHandler(
      SharafHandler.exceptions(
        SharafHandler.cors(
          SharafHandler.routes(routes, cpResHandler),
          corsSettings
        ),
        exceptionMapper
      )
    )
    new UndertowSharafServer(host, port, finalHandler)
  }

}
