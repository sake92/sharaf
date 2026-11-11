package ba.sake.sharaf.handlers

import java.time.Duration
import sttp.client4.quick.*
import ba.sake.sharaf.*

abstract class AbstractSessionHandlerTest extends munit.FunSuite with ServerFixture {

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
    case GET -> Path("invalidate-and-fail") =>
      Session.current.invalidate()
      throw RuntimeException("expected failure")
    case GET -> Path("regenerate-session") =>
      Session.current.regenerate()
      Response.default
    case GET -> Path("session-id") =>
      Response.withBody(Session.current.id)
  }

  private final class StatefulClient(baseUrl: String) {
    private val template = request
    private var sessionCookie: Option[String] = None

    def send(path: String): String = {
      // Build URL with separate path segments to avoid encoding slashes
      val fullUrl = path.split("/").foldLeft(uri"$baseUrl")(_.addPath(_))
      val request = sessionCookie
        .fold(template.get(fullUrl)) { cookieHeader =>
          template.get(fullUrl).header("Cookie", cookieHeader)
        }
      val response = request.send(httpBackend)
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

  private def withStatefulClient(check: StatefulClient => Unit): Unit = {
    val handler = SharafHandler.sessions(SharafHandler.routes(routes), config = SessionConfig.default.withSecure(false))
    withHandler(handler)(base => check(new StatefulClient(base)))
  }

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

  test("invalidation removes the cookie at its configured path") {
    val config = SessionConfig.default.withSecure(false).withCookiePath("/account")
    val handler = SharafHandler.sessions(SharafHandler.routes(routes), config = config)
    withHandler(handler) { base =>
      val response = request.get(uri"$base/invalidate-session").send(httpBackend)
      val cookie = response.header("Set-Cookie").getOrElse(fail("Missing Set-Cookie"))
      assert(cookie.contains("Path=/account"), cookie)
    }
  }

  test("invalidation deletes the stored session when the handler fails") {
    val store = InMemorySessionStore(SessionConfig.default)
    val handler = SharafHandler.sessions(SharafHandler.routes(routes), store = Some(store))
    withHandler(handler) { base =>
      val initial = request.get(uri"$base/session-id").send(httpBackend)
      val id = initial.body
      assert(store.load(id).nonEmpty)
      request.get(uri"$base/invalidate-and-fail").header("Cookie", s"SESSION=$id").send(httpBackend)
      assertEquals(store.load(id), None)
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

  test("default session store uses the configured absolute timeout") {
    val config = SessionConfig.default.withSecure(false).withAbsoluteTimeout(Some(Duration.ofSeconds(-1)))
    val handler = SharafHandler.sessions(SharafHandler.routes(routes), config = config)
    withHandler(handler) { base =>
      val client = new StatefulClient(base)
      client.send("set-session-value/value1")
      assertEquals(client.send("getopt-session-value"), "not found")
    }
  }

  test("an explicitly supplied session store keeps its own expiration policy") {
    val config = SessionConfig.default.withSecure(false).withAbsoluteTimeout(Some(Duration.ofSeconds(-1)))
    val store = InMemorySessionStore(SessionConfig.default)
    val handler = SharafHandler.sessions(SharafHandler.routes(routes), store = Some(store), config = config)
    withHandler(handler) { base =>
      val client = new StatefulClient(base)
      client.send("set-session-value/value1")
      assertEquals(client.send("getopt-session-value"), "value1")
    }
  }
}
