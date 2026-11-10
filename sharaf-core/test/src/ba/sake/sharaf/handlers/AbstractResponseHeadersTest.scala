package ba.sake.sharaf.handlers

import sttp.client4.quick.*
import sttp.model.StatusCode
import ba.sake.sharaf.*
import ba.sake.tupson.*

abstract class AbstractResponseHeadersTest extends munit.FunSuite with ServerFixture {

  private case class Greeting(message: String) derives JsonRW

  test("JSON body headers can be preserved, overridden or removed") {
    for scenario <- Seq(
        (path = "json", expected = Seq("application/json; charset=utf-8")),
        (path = "override", expected = Seq("application/vnd.example+json")),
        (path = "remove", expected = Seq.empty[String])
      )
    do {
      val routes = Routes {
        case GET -> Path("json") => Response.withBody(Greeting("Hello Scala"))
        case GET -> Path("override") =>
          Response.withBody(Greeting("Hello Scala")).settingHeader("content-type", "application/vnd.example+json")
        case GET -> Path("remove") => Response.withBody(Greeting("Hello Scala")).removingHeader("CONTENT-TYPE")
      }
      withServer(routes, ExceptionMapper.default) { base =>
        val response = request.get(uri"$base/${scenario.path}").send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body.parseJson[Greeting], Greeting("Hello Scala"))
        assertEquals(response.headers("Content-Type"), scenario.expected)
      }
    }
  }

  private val scenarios = Seq(
    (name = "body headers", response = Response.withBody("hello"), expected = Seq("text/plain; charset=utf-8")),
    (
      name = "override body headers",
      response = Response.withBody("hello").settingHeader("content-TYPE", "text/custom"),
      expected = Seq("text/custom")
    ),
    (
      name = "remove body headers",
      response = Response.withBody("hello").removingHeader("CONTENT-type"),
      expected = Seq.empty[String]
    ),
    (
      name = "empty values remove body headers",
      response = Response.withBody("hello").settingHeader("Content-Type", Seq.empty),
      expected = Seq.empty[String]
    ),
    (
      name = "last update wins",
      response = Response
        .withBody("hello")
        .settingHeader("Content-Type", "text/old")
        .removingHeader("content-type")
        .settingHeader("CONTENT-TYPE", "text/final"),
      expected = Seq("text/final")
    )
  )

  scenarios.foreach { scenario =>
    test(scenario.name) {
      val routes = Routes { case GET -> Path() => scenario.response }
      withServer(routes, ExceptionMapper.default) { baseUrl =>
        val response = request.get(uri"$baseUrl/").send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body, "hello")
        assertEquals(response.headers("Content-Type"), scenario.expected)
      }
    }
  }

  Seq(true, false).foreach { withBody =>
    test(s"repeated values and ordered updates with body=$withBody") {
      val base = if (withBody) Response.withBody("hello") else Response.default
      val result = base
        .settingHeader("X-Values", Seq("old", "discarded"))
        .settingHeader("x-values", Seq("first", "second"))
        .settingHeader("X-Removed", "old")
        .removingHeader("x-removed")
        .settingHeader("Set-Cookie", Seq("first=one", "second=two"))
        .settingCookie(Cookie("third", "three"))
      val routes = Routes { case GET -> Path() => result }
      withServer(routes, ExceptionMapper.default) { baseUrl =>
        val response = request.get(uri"$baseUrl/").send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body, if (withBody) "hello" else "")
        assertEquals(response.headers("X-Values").sorted, Seq("first", "second"))
        assertEquals(response.headers("X-Removed"), Seq.empty)
        assertEquals(response.headers("Set-Cookie").toSet, Set("first=one", "second=two", "third=three"))
      }
    }
  }
}
