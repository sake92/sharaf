package ba.sake.sharaf

import ba.sake.tupson.*
import ba.sake.sharaf.routing.*

@main def sharafMain: Unit = {

  // TODO middleware, npr da se mogu dodat headeri:
  // PRIJE: Request=>Request
  // POSLIJE: Response=>Response

  val routes1: Routes = {
    case (GET(), Path("users", int(userId)), ?("a" -> q(int(a)))) =>
      Response(s"userId=$userId, a=$a")
  }

  val routes2: Routes = { case (POST(), Path("users"), _) =>
    val body = Request.current.bodyJson[CreateUser]
    Response.json(body)
  }

  HttpServer
    .of(routes1.orElse(routes2))
    .withPort(8181)
    .start()

}

case class CreateUser(name: String) derives JsonRW
