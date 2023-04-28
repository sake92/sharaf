package ba.sake.sharaf

import ba.sake.validation.*
import ba.sake.tupson.*
import ba.sake.sharaf.routing.*
import io.undertow.util.HttpString
import io.undertow.util.Methods
import java.util.UUID
import org.typelevel.jawn.ast.JValue
import java.{util => ju}
import org.typelevel.jawn.ast.JString
import io.undertow.Undertow
import ba.sake.sharaf.handlers.ErrorHandler
import ba.sake.sharaf.handlers.RoutesHandler

@main def sharafMain: Unit = {

  val routes: Routes = { case (GET(), Path(""), _) =>
    Response.json("")
  }

  val handler = ErrorHandler(
    RoutesHandler(routes)
  )

  val server = Undertow
    .builder()
    .addHttpListener(8181, "localhost")
    .setHandler(
      handler
    )
    .build()
  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")

}

////////// Todo MVC backend
given JsonRW[UUID] = new {

  override def write(value: ju.UUID): JValue = JString(value.toString())

  override def parse(path: String, jValue: JValue): ju.UUID = jValue match
    case JString(s) => UUID.fromString(s)
    case _          => throw TupsonException("Expected a UUID string")

}

case class CreateUser(name: String, address: CreateAddress) derives JsonRW {
  validate(
    check(name).is(!_.isBlank, "must not be blank"),
    check(name).is(_.length >= 3, "must be >= 3")
  )
}

case class CreateAddress(name: String) derives JsonRW {
  validate(
    check(name).is(!_.isBlank, "must not be blank"),
    check(name).is(_.length >= 3, "must be >= 3")
  )
}

case class UserQuery(uuid: UUID, age: Option[Int]) derives FromQueryString
