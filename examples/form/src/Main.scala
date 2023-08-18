package demo

import java.nio.file.Files

import io.undertow.Undertow

import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
import ba.sake.validson.*

@main def main: Unit = {

  val server = FormApiServer(8181).server
  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")

}

class FormApiServer(port: Int) {
  private val routes: Routes = { case POST() -> Path("form") =>
    val req = Request.current.bodyForm[CreateCustomerForm].validateOrThrow
    val fileAsString = Files.readString(req.file)
    Response.withBody(CreateCustomerResponse(req.address.street, fileAsString))
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(ErrorHandler(RoutesHandler(routes)))
    .build()
}
