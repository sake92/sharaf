package ba.sake.sharaf

import io.undertow.Undertow
import io.undertow.server.HttpHandler
import ba.sake.sharaf.handlers.SharafHandler
import ba.sake.sharaf.undertow.UndertowSharafRoutes

class UndertowSharafServer private (host: String, port: Int, sharafHandler: SharafHandler) {

  private val server = Undertow
    .builder()
    .addHttpListener(port, host)
    .setHandler(sharafHandler)
    .build()

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()

  def withCorsSettings(corsSettings: CorsSettings): UndertowSharafServer =
    val newHandler = sharafHandler.withCorsSettings(corsSettings)
    copy(sharafHandler = newHandler)
  

  def withExceptionMapper(exceptionMapper: ExceptionMapper): UndertowSharafServer =
    val newHandler = sharafHandler.withExceptionMapper(exceptionMapper)
    copy(sharafHandler = newHandler)

  def withNotFoundHandler(notFoundHandler: Request => Response[?]): UndertowSharafServer =
    val newHandler = sharafHandler.withNotFoundHandler(notFoundHandler)
    copy(sharafHandler = newHandler)

  private def copy(
                    sharafHandler: SharafHandler = sharafHandler
                  ) = new UndertowSharafServer(host, port, sharafHandler)
}

object UndertowSharafServer {
  def apply(host: String, port: Int, sharafHandler: SharafHandler) = new UndertowSharafServer(host, port, sharafHandler)
  def apply(host: String, port: Int, routes: UndertowSharafRoutes) = new UndertowSharafServer(host, port, SharafHandler(routes))
}
