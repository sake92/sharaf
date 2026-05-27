package ba.sake.sharaf.handlers

import sttp.client4.quick.*
import sttp.client4.WebSocketSyncBackend
import sttp.client4.httpclient.HttpClientSyncBackend
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

  private def withStatefulBackend[T](f: WebSocketSyncBackend => T): T = {
    val cookieHandler = new java.net.CookieManager()
    val javaClient = java.net.http.HttpClient.newBuilder().cookieHandler(cookieHandler).build()
    val statefulBackend = HttpClientSyncBackend.usingClient(javaClient)
    f(statefulBackend)
  }

  private def send(path: String, backend: WebSocketSyncBackend): String =
    quickRequest.get(uri"${baseUrl}/$path").send(backend).body

  test("getOpt returns 'not found' when key is absent") {
    withStatefulBackend { backend =>
      assertEquals(send("getopt-session-value", backend), "not found")
    }
  }

  test("set then get session value across requests") {
    withStatefulBackend { backend =>
      send("getopt-session-value", backend)
      send("set-session-value/value1", backend)
      val body = send("get-session-value", backend)
      assertEquals(body, "value1")
    }
  }

  test("invalidate clears session data") {
    withStatefulBackend { backend =>
      send("set-session-value/value1", backend)
      send("get-session-value", backend)
      send("invalidate-session", backend)
      val body = send("getopt-session-value", backend)
      assertEquals(body, "not found")
    }
  }

  test("regenerate preserves data but issues a new session ID") {
    withStatefulBackend { backend =>
      send("set-session-value/myValue", backend)
      val idBefore = send("session-id", backend)
      send("regenerate-session", backend)
      val idAfter = send("session-id", backend)
      assertNotEquals(idBefore, idAfter)
      val data = send("get-session-value", backend)
      assertEquals(data, "myValue")
    }
  }
}
