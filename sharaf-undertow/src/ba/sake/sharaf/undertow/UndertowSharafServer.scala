package ba.sake.sharaf.undertow

import io.undertow.{Undertow, UndertowOptions}
import io.undertow.server.handlers.BlockingHandler
import ba.sake.sharaf.*

class UndertowSharafServer(host: String, listenPort: Int, handler: SharafUndertowHandler) {

  private val finalHandler = BlockingHandler(handler)
  private val server = Undertow
    .builder()
    .addHttpListener(listenPort, host)
    .setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, java.lang.Boolean.TRUE)
    .setHandler(finalHandler)
    .build()

  /** The bound port; available after the server starts. */
  def port: Int = server.getListenerInfo.get(0).getAddress.asInstanceOf[java.net.InetSocketAddress].getPort

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}

object UndertowSharafServer {

  def apply(host: String, handler: SharafHandler): UndertowSharafServer = apply(host, 0, handler)

  def apply(host: String, routes: Routes): UndertowSharafServer = apply(host, 0, routes)

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
