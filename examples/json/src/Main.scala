package demo

import java.util.UUID
import io.undertow.Undertow

import ba.sake.tupson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*

var db = Seq.empty[CustomerRes]

@main def main: Unit = {

  val routes: Routes = {
    case (GET(), Path("customers", uuid(id)), _) =>
      val customerOpt = db.find(_.id == id)
      Response.withBody(customerOpt, s"Customer with id=$id")

    case (GET(), Path("customers"), q[UserQuery](query)) =>
      val customers = db.filter(c => query.name.contains(c.name))
      Response.withBody(customers)

    case (POST(), Path("customers"), _) =>
      val req = Request.current.bodyJson[CreateCustomerReq]
      val res = CustomerRes(UUID.randomUUID(), req.name, AddressRes(req.address.street))
      db = db.appended(res)
      Response.withBody(res)
  }

  val server = Undertow
    .builder()
    .addHttpListener(8181, "localhost")
    .setHandler(
      ErrorHandler(
        RoutesHandler(routes)
      )
    )
    .build()

  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")

}

case class UserQuery(name: Set[String]) derives FromQueryString
