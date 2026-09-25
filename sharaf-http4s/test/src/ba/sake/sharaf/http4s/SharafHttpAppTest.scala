package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import ba.sake.sharaf.http4s.*
import cats.effect.unsafe.implicits.global
import cats.effect.IO
import org.http4s.client.*
import org.http4s.{Method, Request as HRequest, Uri, UrlForm}

class SharafHttpAppTest extends munit.FunSuite {

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
      Response.withBody(form("tag").map {
        case ba.sake.formson.FormValue.Str(value) => value
        case _ => ""
      }.mkString(","))
    }))
    val request = HRequest[IO](method = Method.POST, uri = Uri.unsafeFromString("http://localhost/form"))
      .withEntity(UrlForm("tag" -> "one", "tag" -> "two"))

    val response = Client.fromHttpApp(app).expect[String](request).unsafeRunSync()
    assertEquals(response, "one,two")
  }
}
