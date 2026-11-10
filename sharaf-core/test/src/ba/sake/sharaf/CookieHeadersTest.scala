package ba.sake.sharaf

import java.time.Instant

class CookieHeadersTest extends munit.FunSuite {
  test("request parsing preserves repeated names, headers and opaque values") {
    val cookies = CookieHeaders.parse(Seq("a=one; a=two; token=a=b==; opaque=a+b%2Fc", "other=three"))
    assertEquals(
      cookies,
      Seq(
        Cookie("a", "one"),
        Cookie("a", "two"),
        Cookie("token", "a=b=="),
        Cookie("opaque", "a+b%2Fc"),
        Cookie("other", "three")
      )
    )
  }

  test("empty separators and names are ignored; empty and bare values are retained") {
    assertEquals(
      CookieHeaders.parse(Seq(" ; empty=; bare; =ignored; ; spaced = value ;")),
      Seq(Cookie("empty", ""), Cookie("bare", ""), Cookie("spaced", "value"))
    )
    assertEquals(CookieHeaders.parse(Seq.empty), Seq.empty)
    assertEquals(CookieHeaders.parse(Seq("", "; ;")), Seq.empty)
  }

  test("quoted values are preserved without URL decoding") {
    assertEquals(
      CookieHeaders.parse(Seq("quoted=\"abc==\"; escaped=%22abc%22")),
      Seq(Cookie("quoted", "\"abc==\""), Cookie("escaped", "%22abc%22"))
    )
  }

  test("render modern attributes and GMT expiry") {
    val cookie = Cookie(
      "session",
      "abc==",
      path = Some("/app"),
      domain = Some("example.com"),
      maxAge = Some(123),
      expires = Some(Instant.parse("2030-01-02T03:04:05Z")),
      secure = true,
      httpOnly = true,
      sameSiteMode = Some("Strict")
    )
    assertEquals(
      CookieHeaders.render(cookie),
      "session=abc==; Path=/app; Domain=example.com; Max-Age=123; Expires=Wed, 2 Jan 2030 03:04:05 GMT; Secure; HttpOnly; SameSite=Strict"
    )
  }

  test("minimal and deletion cookies") {
    assertEquals(CookieHeaders.render(Cookie("empty", "")), "empty=")
    val removal = Response.removingCookie("session").cookieUpdates.updates.head
    assertEquals(CookieHeaders.render(removal), "session=; Expires=Thu, 1 Jan 1970 00:00:00 GMT")
  }
}
