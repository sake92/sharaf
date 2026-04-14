package ba.sake.sharaf.handlers

import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

abstract class AbstractSessionHandlerTest extends munit.FunSuite {

  val port: Int = NetworkUtils.getFreePort()
  def baseUrl: String = s"http://localhost:${port}"

  def startServer(): Unit
  def stopServer(): Unit

  override def beforeAll(): Unit = startServer()
  override def afterAll(): Unit = stopServer()

  val routes = Routes {
    case GET -> Path("getopt-session-value") =>
      val value = Session.current.getOpt[String]("key1")
      Response.withBody(value.getOrElse("not found"))
    case GET -> Path("get-session-value") =>
      val value = Session.current.get[String]("key1")
      Response.withBody(value)
    case GET -> Path("set-session-value", value) =>
      Session.current.set("key1", value)
      Response.default
    case GET -> Path("invalidate-session") =>
      Session.current.invalidate()
      Response.default
    case GET -> Path("regenerate-session") =>
      Session.current.regenerate()
      Response.default
    case GET -> Path("session-id") =>
      Response.withBody(Session.current.id)
  }

  /** Extracts the raw `name=value` token from a `Set-Cookie` header value. */
  private def sessionCookie(setCookieHeader: String): String =
    setCookieHeader.split(";").head.trim

  /** Sends a request with no cookie and returns (body, cookie). */
  private def sendAndGetCookie(path: String): (String, Option[String]) = {
    val res = quickRequest.get(uri"${baseUrl}/$path").send()
    val cookie = res.header("Set-Cookie").map(sessionCookie)
    (res.body, cookie)
  }

  /** Sends a request carrying the given cookie and returns (body, new cookie). */
  private def sendWithCookie(path: String, cookie: String): (String, Option[String]) = {
    val res = quickRequest.get(uri"${baseUrl}/$path").header("Cookie", cookie).send()
    val newCookie = res.header("Set-Cookie").map(sessionCookie)
    (res.body, newCookie)
  }

  test("getOpt returns 'not found' when key is absent") {
    val (body, _) = sendAndGetCookie("getopt-session-value")
    assertEquals(body, "not found")
  }

  test("set then get session value across requests") {
    val (_, cookie1) = sendAndGetCookie("getopt-session-value")
    val cookie = cookie1.getOrElse(fail("expected session cookie"))
    locally {
      sendWithCookie("set-session-value/value1", cookie)
    }
    val (body, _) = sendWithCookie("get-session-value", cookie)
    assertEquals(body, "value1")
  }

  test("invalidate clears session data") {
    val (_, cookie1) = sendAndGetCookie("set-session-value/value1")
    val cookie = cookie1.getOrElse(fail("expected session cookie"))
    locally {
      sendWithCookie("get-session-value", cookie)
    }
    // Invalidate the session
    val (_, _) = sendWithCookie("invalidate-session", cookie)
    // Old cookie is no longer valid — getOpt should return "not found"
    val (body, _) = sendWithCookie("getopt-session-value", cookie)
    assertEquals(body, "not found")
  }

  test("regenerate preserves data but issues a new session ID") {
    val (_, cookie1) = sendAndGetCookie("set-session-value/myValue")
    val cookie = cookie1.getOrElse(fail("expected session cookie"))
    val (idBefore, _) = sendWithCookie("session-id", cookie)
    val (_, newCookie1) = sendWithCookie("regenerate-session", cookie)
    val newCookie = newCookie1.getOrElse(fail("expected new session cookie after regenerate"))
    val (idAfter, _) = sendWithCookie("session-id", newCookie)
    assertNotEquals(idBefore, idAfter)
    val (data, _) = sendWithCookie("get-session-value", newCookie)
    assertEquals(data, "myValue")
  }
}
