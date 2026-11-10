package ba.sake.sharaf.handlers

import java.time.{Instant, ZonedDateTime}
import java.time.format.DateTimeFormatter
import sttp.client4.quick.*
import sttp.model.StatusCode
import ba.sake.sharaf.*

abstract class AbstractCookieTest extends munit.FunSuite {
  def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit

  private val request = quickRequest.httpVersion(sttp.model.HttpVersion.HTTP_1_1)

  private val routes = Routes {
    case GET -> Path("request") =>
      val pairs = Request.current.cookies.map(cookie => s"${cookie.name}=${cookie.value}")
      Response.withBody(pairs.sorted.mkString("|"))
    case GET -> Path("response") =>
      Response
        .withBody("cookies")
        .settingHeader("Set-Cookie", "existing=kept")
        .settingCookie(
          Cookie(
            "session",
            "abc==",
            path = Some("/app"),
            domain = Some("example.com"),
            maxAge = Some(123),
            expires = Some(Instant.parse("2030-01-02T03:04:05Z")),
            secure = true,
            httpOnly = true,
            sameSite = true,
            sameSiteMode = Some("Strict")
          )
        )
        .settingCookie(Cookie("theme", "dark"))
    case GET -> Path("remove") => Response.removingCookie("session").settingCookie(Cookie("empty", ""))
  }

  test("request cookies retain opaque values") {
    withServer(routes, ExceptionMapper.default) { baseUrl =>
      val response = request
        .get(uri"$baseUrl/request")
        .header("Cookie", "token=a=b==; empty=; opaque=a+b%2Fc; other=two")
        .send()
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "empty=|opaque=a+b%2Fc|other=two|token=a=b==")
    }
  }

  test("multiple Cookie headers are read") {
    withServer(routes, ExceptionMapper.default) { baseUrl =>
      val response = request
        .get(uri"$baseUrl/request")
        .header("Cookie", "first=one")
        .header("Cookie", "second=two", sttp.client4.DuplicateHeaderBehavior.Add)
        .send()
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "first=one|second=two")
    }
  }

  test("absent cookies") {
    withServer(routes, ExceptionMapper.default) { baseUrl =>
      val response = request.get(uri"$baseUrl/request").send()
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "")
    }
  }

  test("response cookies preserve attributes and existing headers") {
    withServer(routes, ExceptionMapper.default) { baseUrl =>
      val response = request.get(uri"$baseUrl/response").send()
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "cookies")
      val cookies = response.headers.filter(_.is("Set-Cookie")).map(_.value)
      assertEquals(cookies.size, 3)
      assert(cookies.contains("existing=kept"))
      assert(cookies.contains("theme=dark"))
      val session = cookies.find(_.startsWith("session=")).get.split("; *").toSet
      val expiry = session.find(_.startsWith("Expires=")).get.stripPrefix("Expires=")
      assertEquals(
        ZonedDateTime.parse(expiry, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant,
        Instant.parse("2030-01-02T03:04:05Z")
      )
      assertEquals(
        session.filterNot(_.startsWith("Expires=")),
        Set(
          "session=abc==",
          "Path=/app",
          "Domain=example.com",
          "Max-Age=123",
          "Secure",
          "HttpOnly",
          "SameSite=Strict"
        )
      )
    }
  }

  test("cookies can be removed without a response body") {
    withServer(routes, ExceptionMapper.default) { baseUrl =>
      val response = request.get(uri"$baseUrl/remove").send()
      assertEquals(response.code, StatusCode.Ok)
      val cookies = response.headers.filter(_.is("Set-Cookie")).map(_.value)
      assertEquals(cookies.size, 2)
      assert(cookies.contains("empty="))
      val removed = cookies.find(_.startsWith("session=")).get.split("; *").toSet
      val expiry = removed.find(_.startsWith("Expires=")).get.stripPrefix("Expires=")
      assertEquals(ZonedDateTime.parse(expiry, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant, Instant.EPOCH)
      assertEquals(removed.filterNot(_.startsWith("Expires=")), Set("session="))
    }
  }
}
