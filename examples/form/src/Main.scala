package demo

import java.nio.file.Files
import io.undertow.Undertow
import ba.sake.sharaf.*, handlers.*, routing.*

@main def main: Unit =
  val module = FormApiModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class FormApiModule(port: Int) {

  val baseUrl = s"http://localhost:${port}"

  private val routes: Routes = { case POST() -> Path("form") =>
    val req = Request.current.bodyFormValidated[CreateCustomerForm]
    val fileAsString = Files.readString(req.file)
    Response.withBody(CreateCustomerResponse(req.address.street, fileAsString))
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(ErrorHandler(RoutesHandler(routes)))
    .build()
}
