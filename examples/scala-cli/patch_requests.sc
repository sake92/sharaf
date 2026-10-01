//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.20.0
//> using dep ba.sake::tupson:0.31.0

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer
import ba.sake.tupson.{given, *}
import org.typelevel.jawn.ast.*

enum Patch[+T]:
  case Set(value: T)
  case Keep

object Patch:
  given [T](using valueRW: JsonRW[T]): JsonRW[Patch[T]] with
    override def write(value: Patch[T]): JValue = value match
      case Set(value) => valueRW.write(value)
      case Keep       => throw TupsonException("Patch.Keep can only be written as an object field")

    override def shouldWriteField(value: Patch[T]): Boolean = value != Keep

    override def parse(path: String, jValue: JValue): Patch[T] =
      Set(valueRW.parse(path, jValue))

    override def default: Option[Patch[T]] = Some(Keep)

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

UndertowSharafServer("localhost", 8181, routes, exceptionMapper = ExceptionMapper.json).start()
println("Server started at http://localhost:8181")
