package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import ba.sake.sharaf.http4s.*
import cats.effect.unsafe.implicits.global
import cats.effect.IO
import org.http4s.client.*
import org.http4s.{Method, Request as HRequest, Uri, UrlForm}
import org.typelevel.ci.CIString
import ba.sake.tupson.*

class SharafHttpAppTest extends munit.FunSuite {

  private case class Greeting(message: String) derives JsonRW

  test("body content type is forwarded and explicit header updates take precedence") {
    val routes = Routes {
      case GET -> Path("json") => Response.withBody(Greeting("Hello Scala"))
      case GET -> Path("text") => Response.withBody("Hello Scala")
      case GET -> Path("override") =>
        Response.withBody(Greeting("Hello Scala")).settingHeader("content-type", "application/vnd.example+json")
      case GET -> Path("remove") => Response.withBody(Greeting("Hello Scala")).removingHeader("CONTENT-TYPE")
    }
    val client = Client.fromHttpApp(SharafHttpApp(SharafHandler.routes(routes)))
    def check(path: String, contentType: Option[String], json: Boolean): Unit =
      client
        .run(HRequest[IO](uri = Uri.unsafeFromString(s"http://localhost/$path")))
        .use { response =>
          response.bodyText.compile.string.map { body =>
            assertEquals(response.status.code, 200)
            val contentTypes = response.headers.headers.filter(_.name == CIString("Content-Type")).map(_.value)
            assertEquals(contentTypes, contentType.toList)
            if json then assertEquals(body.parseJson[Greeting], Greeting("Hello Scala"))
            else assertEquals(body, "Hello Scala")
          }
        }
        .unsafeRunSync()

    check("json", Some("application/json; charset=utf-8"), true)
    check("text", Some("text/plain; charset=utf-8"), false)
    check("override", Some("application/vnd.example+json"), true)
    check("remove", None, true)
  }

  test("Hello") {
    val app = SharafHttpApp(SharafHandler.routes(Routes { case GET -> Path("hello") =>
      Response.withBody("Hello World!")
    }))

    val response = Client.fromHttpApp(app).expect[String]("http://localhost:8080/hello").unsafeRunSync()

    assertEquals(response, "Hello World!")
  }

  test("URL encoded form fields reach Sharaf routes") {
    val app = SharafHttpApp(SharafHandler.routes(Routes { case POST -> Path("form") =>
      val form = Request.current.bodyFormRaw
      Response.withBody(
        form("tag")
          .map {
            case ba.sake.formson.FormValue.Str(value) => value
            case _                                    => ""
          }
          .mkString(",")
      )
    }))
    val request = HRequest[IO](method = Method.POST, uri = Uri.unsafeFromString("http://localhost/form"))
      .withEntity(UrlForm("tag" -> "one", "tag" -> "two"))

    val response = Client.fromHttpApp(app).expect[String](request).unsafeRunSync()
    assertEquals(response, "one,two")
  }
}
