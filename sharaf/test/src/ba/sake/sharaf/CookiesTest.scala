package ba.sake.sharaf

import ba.sake.sharaf.routing.*
import io.undertow.Undertow

class CookiesTest extends munit.FunSuite {

  val port = utils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("settingCookie") =>
      Response.settingCookie(Cookie("cookie1", "cookie1Value"))
    case GET -> Path("removingCookie") =>
      Response.removingCookie("cookie1")
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("settingCookie sets a cookie") {
    val res = requests.get(s"${baseUrl}/settingCookie")
    val cookie = res.cookies("cookie1")
    assertEquals(cookie.getValue, "cookie1Value")
    assertEquals(cookie.getMaxAge, -1L)
  }
  
  test("removingCookie removes a cookie (sets value to empty and expires to min)") {
    val session = requests.Session()
    session.get(s"${baseUrl}/settingCookie") // first set it
    session.get(s"${baseUrl}/removingCookie")
    // for some reason requests parses it as double quotes.. IDK
    val cookie = session.cookies("cookie1")
    assertEquals(cookie.getValue, """ "" """.trim)
    assertEquals(cookie.getMaxAge, 0L) // expired
  }

}
