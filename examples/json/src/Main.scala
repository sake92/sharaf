package demo

import java.util.UUID
import io.undertow.Undertow

import ba.sake.sharaf.*, handlers.*, routing.*
import ba.sake.validson.*

@main def main: Unit =
  val module = JsonApiModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class JsonApiModule(port: Int) {

  val baseUrl = s"http://localhost:${port}"

  // don't do this at home!
  private var db = Seq.empty[CustomerRes]

  private val routes: Routes = {
    case GET() -> Path("customers", param[UUID](id)) =>
      val customerOpt = db.find(_.id == id)
      Response.withBodyOpt(customerOpt, s"Customer with id=$id")

    case GET() -> Path("customers") =>
      val query = Request.current.queryParams[UserQuery].validateOrThrow
      val customers = if query.name.isEmpty then db else db.filter(c => query.name.contains(c.name))
      Response.withBody(customers)

    case POST() -> Path("customers") =>
      val req = Request.current.bodyJson[CreateCustomerReq].validateOrThrow
      val res = CustomerRes(UUID.randomUUID(), req.name, AddressRes(req.address.street))
      db = db.appended(res)
      Response.withBody(res)
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(ErrorHandler(RoutesHandler(routes), ErrorMapper.json))
    .build()
}
