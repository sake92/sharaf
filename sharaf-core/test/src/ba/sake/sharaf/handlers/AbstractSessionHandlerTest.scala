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

  private final class StatefulClient {
    private var sessionCookie: Option[String] = None

    def send(path: String): String = {
      // Build URL with separate path segments to avoid encoding slashes
      val fullUrl = path.split("/").foldLeft(uri"$baseUrl")(_.addPath(_))
      val request = sessionCookie
        .fold(quickRequest.get(fullUrl)) { cookieHeader =>
          quickRequest.get(fullUrl).header("Cookie", cookieHeader)
        }
      val response = request.send()
      response.header("Set-Cookie").foreach { setCookieHeader =>
        val cookiePair = setCookieHeader
          .split(";", 2)
          .headOption
          .map(_.trim)
          .filter(_.contains("="))
          .getOrElse(throw RuntimeException(s"Malformed Set-Cookie header: $setCookieHeader"))
        sessionCookie = Some(cookiePair)
      }
      response.body
    }
  }

  private def withStatefulClient[T](f: StatefulClient => T): T =
    f(new StatefulClient)

  test("getOpt returns 'not found' when key is absent") {
    withStatefulClient { client =>
      assertEquals(client.send("getopt-session-value"), "not found")
    }
  }

  test("set then get session value across requests") {
    withStatefulClient { client =>
      client.send("getopt-session-value")
      client.send("set-session-value/value1")
      val body = client.send("get-session-value")
      assertEquals(body, "value1")
    }
  }

  test("invalidate clears session data") {
    withStatefulClient { client =>
      client.send("set-session-value/value1")
      client.send("get-session-value")
      client.send("invalidate-session")
      val body = client.send("getopt-session-value")
      assertEquals(body, "not found")
    }
  }

  test("regenerate preserves data but issues a new session ID") {
    withStatefulClient { client =>
      client.send("set-session-value/myValue")
      val idBefore = client.send("session-id")
      client.send("regenerate-session")
      val idAfter = client.send("session-id")
      assertNotEquals(idBefore, idAfter)
      val data = client.send("get-session-value")
      assertEquals(data, "myValue")
    }
  }
}
