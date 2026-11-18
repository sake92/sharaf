//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer
import ba.sake.tupson.{given, *}

case class User(name: String, address: Option[String]) derives JsonRW
case class UserPatch(name: Patch[String], address: Patch[Option[String]]) derives JsonRW

object Users:
  private var user = User("Grace", Some("Arlington"))

  def current: User = user

  def patch(update: UserPatch): User =
    user = User(
      name = update.name match
        case Patch.Set(value) => value
        case Patch.Keep       => user.name,
      address = update.address match
        case Patch.Set(value) => value
        case Patch.Keep       => user.address,
    )
    user

val routes = Routes:
  case GET -> Path("user") => Response.withBody(Users.current)

  case PATCH -> Path("user") =>
    Response.withBody(Users.patch(Request.current.bodyJson[UserPatch]))

PotatehSharafServer(routes, host = "localhost", port = 8181, exceptionMapper = ExceptionMapper.json).start()
println("Server started at http://localhost:8181")
