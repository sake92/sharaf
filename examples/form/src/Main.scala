package demo

import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
import io.undertow.Undertow
import java.nio.file.Files

@main def main: Unit = {

  val server = FormApiServer(8181).server
  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")

}

class FormApiServer(port: Int) {
  private val routes: Routes = { case POST() -> Path("form") =>
    val req = Request.current.bodyForm[CreateCustomerForm]
    println(s"Got form request: $req")
    val fileAsString = Files.readString(req.file)
    Response.withBody(CreateCustomerResponse(fileAsString))
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(RoutesHandler(routes))
    .build()
}
