package ba.sake.sharaf

import io.undertow.Undertow
import io.undertow.util.HttpString

import ba.sake.sharaf.handlers.*
import io.undertow.server.HttpHandler

type RequestParams = (HttpString, Path, QueryString)

type Routes = Request ?=> PartialFunction[RequestParams, Response]

// TODO add websocket handler
final class HttpServer(handler: HttpHandler, port: Int) {

  private var undertowServer: Undertow = _

  def withHandler(handler: HttpHandler): HttpServer =
    new HttpServer(handler, port)

  def withPort(port: Int): HttpServer =
    new HttpServer(handler, port)

  def start(): Unit = {
    undertowServer = Undertow
      .builder()
      .addHttpListener(port, "localhost")
      .setHandler(
        handler
      )
      .build()
    undertowServer.start()

    val info = undertowServer.getListenerInfo().get(0)
    val url = s"${info.getProtcol}:/${info.getAddress}"
    println(s"Started HTTP server at $url")
  }

  def stop(): Unit = {
    undertowServer.stop()
  }
}

object HttpServer {
  private val DefaultPort = 9000

  def apply(httpHandler: HttpHandler): HttpServer =
    new HttpServer(
      httpHandler,
      DefaultPort
    )

  def of(routes: Routes): HttpServer =
    new HttpServer(
      ErrorHandler(
        RoutesHandler(routes),
        { case _ if false => Response("should not happen") }
      ),
      DefaultPort
    )

}
