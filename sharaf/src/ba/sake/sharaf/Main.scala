package ba.sake.sharaf

import ba.sake.tupson.*
import ba.sake.sharaf.routing.*
import io.undertow.util.HttpString
import io.undertow.util.Methods

@main def sharafMain: Unit = {

  // TODO middleware, npr da se mogu dodat headeri:
  // PRIJE: Request=>Request
  // POSLIJE: Response=>Response

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
  if name.isBlank then
    throw ValidationException(
      List(
        ValidationError("name", "Name is blank"),
        ValidationError("name", "Name is stupid"),
      )
    )
}

case class UserQuery(name: String, age: Option[Int]) derives FromQueryString
