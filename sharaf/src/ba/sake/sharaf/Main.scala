package ba.sake.sharaf

import ba.sake.tupson.*
import ba.sake.sharaf.routing.*
import io.undertow.util.HttpString
import io.undertow.util.Methods
import java.util.UUID

@main def sharafMain: Unit = {

  val getRoutes: Routes = {
    case (GET(), Path("users", int(userId)), _) =>
      Response(s"userId=$userId")
    case (GET(), Path("users"), q[UserQuery](query)) =>
      Response(s"query=$query")
  }

  val postRoutes: Routes = { case (POST(), Path("users"), _) =>
    val body = Request.current.bodyJson[CreateUser]
    Response.json(body)
  }

  val routes: Routes = getRoutes.orElse(postRoutes)

  HttpServer
    .of(routes)
    .withPort(8181)
    .start()

}

case class CreateUser(name: String) derives JsonRW {
  Validation.assertAll(
    ("name", !name.isBlank, "Name must not be blank"),
    ("name", name.length >= 3, "Name must be 3+ length")
  )
}

case class UserQuery(uuid: UUID, age: Option[Int]) derives FromQueryString
