package ba.sake.sharaf

import io.undertow.Undertow
import io.undertow.util.HttpString

import ba.sake.sharaf.handlers.*

type RequestParams = (HttpString, Path, QueryString)

type Routes = Request ?=> PartialFunction[RequestParams, Response]

final class HttpServer(routes: Routes, port: Int = 9000) {

  // escape hatch..
  var undertowServer: Undertow = _

  def withPort(port: Int): HttpServer =
    new HttpServer(routes, port)

  def start(): Unit = {
    undertowServer = Undertow
      .builder()
      .addHttpListener(port, "localhost")
      .setHandler(
        new ErrorHandler(
          new RoutesHandler(routes)
        )
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
  def of(routes: Routes): HttpServer =
    new HttpServer(routes)
}
