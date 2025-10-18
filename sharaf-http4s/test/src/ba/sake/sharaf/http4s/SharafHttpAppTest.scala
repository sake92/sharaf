package ba.sake.sharaf.http4s

import ba.sake.sharaf.*
import ba.sake.sharaf.http4s.*
import cats.effect.unsafe.implicits.global
import org.http4s.client.*

class SharafHttpAppTest extends munit.FunSuite {

  test("Hello") {
    val app = SharafHttpApp(SharafHandler.routes(Routes { case GET -> Path("hello") =>
      Response.withBody("Hello World!")
    }))

    val response = Client.fromHttpApp(app).expect[String]("http://localhost:8080/hello").unsafeRunSync()

    assertEquals(response, "Hello World!")
  }
}
