package ba.sake.sharaf.handlers

import sttp.client4.quick.*
import sttp.client4.DuplicateHeaderBehavior
import sttp.model.StatusCode
import ba.sake.sharaf.*

abstract class AbstractDuplicateCookieTest extends munit.FunSuite with ServerFixture {
  private val routes = Routes {
    case GET -> Path("cookies") =>
      val values = Request.current.cookies.filter(_.name == "SESSION").map(_.value)
      Response.withBody(values.mkString("|"))
    case GET -> Path("session") => Response.withBody(Session.current.id)
  }

  test("duplicate cookies retain their values and order") {
    withServer(routes, ExceptionMapper.default) { base =>
      val response = request.get(uri"$base/cookies").header("Cookie", "SESSION=one; SESSION=two").send(httpBackend)
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "one|two")
      val separate = request.get(uri"$base/cookies")
        .header("Cookie", "SESSION=one")
        .header("Cookie", "SESSION=two", DuplicateHeaderBehavior.Add)
        .send(httpBackend)
      assertEquals(separate.body, "one|two")
    }
  }

  test("session lookup uses the first duplicate cookie") {
    withHandler(SharafHandler.sessions(SharafHandler.routes(routes))) { base =>
      val first = request.get(uri"$base/session").send(httpBackend).body
      val second = request.get(uri"$base/session").send(httpBackend).body
      assertNotEquals(first, second)
      for (a, b) <- Seq(first -> second, second -> first) do {
        val response = request.get(uri"$base/session")
          .header("Cookie", s"SESSION=$a; SESSION=$b").send(httpBackend)
        assertEquals(response.body, a)
      }
    }
  }
}
