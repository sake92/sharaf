package ba.sake.sharaf.undertow

import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

class CookiesTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("settingCookie") =>
      Response.settingCookie(Cookie("cookie1", "cookie1Value"))
    case GET -> Path("removingCookie") =>
      Response.removingCookie("cookie1")
  }

  val server = UndertowSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("settingCookie sets a cookie") {
    val cookieHandler = new java.net.CookieManager()
    val javaClient = java.net.http.HttpClient.newBuilder().cookieHandler(cookieHandler).build()
    val statefulBackend = sttp.client4.httpclient.HttpClientSyncBackend.usingClient(javaClient)
    quickRequest.get(uri"${baseUrl}/settingCookie").send(statefulBackend)
    val cookie = cookieHandler.getCookieStore.get(uri"${baseUrl}/getopt-session-value".toJavaUri).iterator().next()
    assertEquals(cookie.getValue, "cookie1Value")
    assertEquals(cookie.getMaxAge, -1L) // does not expire
  }

  test("removingCookie removes a cookie (sets value to empty and expires to min)") {
    val cookieHandler = new java.net.CookieManager()
    val javaClient = java.net.http.HttpClient.newBuilder().cookieHandler(cookieHandler).build()
    val statefulBackend = sttp.client4.httpclient.HttpClientSyncBackend.usingClient(javaClient)
    quickRequest.get(uri"${baseUrl}/settingCookie").send(statefulBackend) // first set it
    quickRequest.get(uri"${baseUrl}/removingCookie").send(statefulBackend)
    // for some reason requests parses it as double quotes.. IDK
    val cookies = cookieHandler.getCookieStore.get(uri"${baseUrl}/getopt-session-value".toJavaUri)
    assert(cookies.isEmpty) // cookie is effectively removed
  }

}
