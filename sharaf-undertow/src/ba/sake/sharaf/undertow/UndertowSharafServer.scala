package ba.sake.sharaf.undertow

import io.undertow.{Undertow, UndertowOptions}
import io.undertow.server.handlers.BlockingHandler
import ba.sake.sharaf.*

class UndertowSharafServer private (
    host: String,
    listenPort: Int,
    handler: SharafUndertowHandler,
    tls: Option[TlsConfig]
) {

  private val finalHandler = BlockingHandler(handler)
  private val server = {
    val builder = Undertow.builder()
    tls match {
      case Some(value) => builder.addHttpsListener(listenPort, host, value.context)
      case None        => builder.addHttpListener(listenPort, host)
    }
    builder
      .setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, java.lang.Boolean.TRUE)
      .setHandler(finalHandler)
      .build()
  }

  /** The bound port; available after the server starts. */
  def port: Int = server.getListenerInfo.get(0).getAddress.asInstanceOf[java.net.InetSocketAddress].getPort

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}

object UndertowSharafServer {

  def apply(
      routes: Routes,
      host: String = "127.0.0.1",
      port: Int = 0,
      corsSettings: CorsSettings = CorsSettings.disabled,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler,
      tls: Option[TlsConfig] = None
  ): UndertowSharafServer = {
    val cpResHandler = SharafHandler.classpathResources(
      "public",
      SharafHandler.classpathResources("META-INF/resources/webjars", notFoundHandler)
    )
    val handler = SharafUndertowHandler(
      SharafHandler.exceptions(
        SharafHandler.cors(
          SharafHandler.routes(routes, cpResHandler),
          corsSettings
        ),
        exceptionMapper
      )
    )
    new UndertowSharafServer(host, port, handler, tls)
  }

  def apply(handler: SharafHandler, host: String, port: Int, tls: Option[TlsConfig]): UndertowSharafServer =
    new UndertowSharafServer(host, port, SharafUndertowHandler(handler), tls)

}
