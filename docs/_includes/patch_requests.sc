//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.23.0

import ba.sake.sharaf.*
import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
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

PotatehSharafServer.start(ServerConfig.Default.withBindAddress(new InetSocketAddress("localhost", 8181)), routes, exceptionMapper = ExceptionMapper.json)
println("Server started at http://localhost:8181")
