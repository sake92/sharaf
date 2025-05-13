package ba.sake.sharaf.undertow

import io.undertow.Undertow
import io.undertow.server.session.{InMemorySessionManager, SessionAttachmentHandler, SessionCookieConfig}
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.handlers.SharafHandler
import ba.sake.sharaf.utils.NetworkUtils

class SessionsTest extends munit.FunSuite {
  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
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

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(
      new SessionAttachmentHandler(
        SharafHandler(routes),
        new InMemorySessionManager("in-memory-session-manager"),
        new SessionCookieConfig()
      )
    )
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Session.set sets a value and Session.get gets it") {
    // cookies are used to track sessions
    val cookieHandler = new java.net.CookieManager()
    val javaClient = java.net.http.HttpClient.newBuilder().cookieHandler(cookieHandler).build()
    val statefulBackend = sttp.client4.httpclient.HttpClientSyncBackend.usingClient(javaClient)
    locally {
      val res = quickRequest.get(uri"${baseUrl}/getopt-session-value").send(statefulBackend)
      assertEquals(res.body, "not found")
    }
    locally {
      quickRequest.get(uri"${baseUrl}/set-session-value/value1").send(statefulBackend)
    }
    locally {
      val res = quickRequest.get(uri"${baseUrl}/get-session-value").send(statefulBackend)
      assertEquals(res.body, "value1")
    }
  }
}
