package ba.sake.sharaf

import io.undertow.Undertow
import io.undertow.server.HttpHandler
import ba.sake.sharaf.handlers.SharafHandler
import ba.sake.sharaf.undertow.UndertowSharafRoutes

class UndertowSharafServer private (host: String, port: Int, httpHandler: HttpHandler) {

  private val server = Undertow
    .builder()
    .addHttpListener(port, host)
    .setHandler(httpHandler)
    .build()

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
}

object UndertowSharafServer {
  def apply(host: String, port: Int, httpHandler: HttpHandler) = new UndertowSharafServer(host, port, httpHandler)
  def apply(host: String, port: Int, routes: UndertowSharafRoutes) = new UndertowSharafServer(host, port, SharafHandler(routes))
}
