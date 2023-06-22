package demo

import java.util.UUID
import io.undertow.Undertow

import ba.sake.tupson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
import ba.sake.querson.*

var db = Seq.empty[CustomerRes]

@main def main: Unit = {

  val server = JsonApiServer(8181).server
  server.start()

  // TODO add a wrapper for this stuff ?
  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started JsonApiServer at $url")
}

class JsonApiServer(port: Int) {
  private val routes: Routes = {
    case (GET(), Path("customers", uuid(id)), _) =>
      val customerOpt = db.find(_.id == id)
      Response.withBodyOpt(customerOpt, s"Customer with id=$id")

    case (GET(), Path("customers"), q[UserQuery](query)) =>
      val customers = if query.name.isEmpty then db else db.filter(c => query.name.contains(c.name))
      Response.withBody(customers)

    case (POST(), Path("customers"), _) =>
      val req = Request.current.bodyJson[CreateCustomerReq]
      val res = CustomerRes(UUID.randomUUID(), req.name, AddressRes(req.address.street))
      db = db.appended(res)
      Response.withBody(res)
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(RoutesHandler(routes))
    .build()
}

case class UserQuery(name: Set[String]) derives QueryStringRW
