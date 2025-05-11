package ba.sake.sharaf.undertow

import io.undertow.Undertow
import io.undertow.server.session.{InMemorySessionManager, SessionAttachmentHandler, SessionCookieConfig}
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.{*, given}
import ba.sake.sharaf.undertow.handlers.SharafHandler

class SessionsTest extends munit.FunSuite {
  val port = utils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = UndertowSharafRoutes {
    case GET -> Path("getopt-session-value") =>
      val key1Value = Session.current.getOpt[String]("key1")
      Response.withBody(key1Value.getOrElse("not found"))
    case GET -> Path("get-session-value") =>
      val key1Value = Session.current.get[String]("key1")
      Response.withBody(key1Value)
    case GET -> Path("set-session-value", value) =>
      Session.current.set("key1", value)
      Response.default
  }

  val server =  Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(
      new SessionAttachmentHandler(
        SharafHandler(routes),
        new InMemorySessionManager("in-memory-session-manager"),
        new SessionCookieConfig()
      )
    ).build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Session.set sets a value and Session.get gets it") {
    // cookies are used to track sessions
    val session = requests.Session()
    locally {
      val res = session.get(s"${baseUrl}/getopt-session-value")
      assertEquals(res.text(), "not found")
    }
    locally {
      session.get(s"${baseUrl}/set-session-value/value1")
    }
    locally {
      val res = session.get(s"${baseUrl}/get-session-value")
      assertEquals(res.text(), "value1")
    }
  }
}
