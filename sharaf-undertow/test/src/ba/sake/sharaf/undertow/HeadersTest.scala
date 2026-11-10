package ba.sake.sharaf.undertow

import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

class HeadersTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("settingHeader") =>
      Response.settingHeader("header1", "header1Value")
    case GET -> Path("removingHeader") =>
      Response.settingHeader("bla", "bla1").removingHeader("bla")
    case GET -> Path("setAndRemove") =>
      Response.settingHeader("header1", "header1Value").removingHeader("header1")
  }

  val server = UndertowSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("settingHeader sets a header") {
    val res = quickRequest.get(uri"${baseUrl}/settingHeader").send()
    assertEquals(res.headers("header1"), Seq("header1Value"))
  }

  test("removingHeader removes a header") {
    val res = quickRequest.get(uri"${baseUrl}/removingHeader").send()
    assertEquals(res.headers("bla"), Seq.empty)
  }

  test("settingHeader and then removingHeader removes a header") {
    val res = quickRequest.get(uri"${baseUrl}/setAndRemove").send()
    assertEquals(res.headers("header1"), Seq.empty)
  }
  test("response updates preserve and remove headers set by an earlier Undertow handler") {
    val enginePort = NetworkUtils.getFreePort()
    val routes = Routes { case GET -> Path() =>
      Response
        .withBody("hello")
        .removingHeader("x-remove")
        .settingHeader("x-empty", Seq.empty)
        .settingHeader("x-replace", Seq("first", "second"))
    }
    val handler = SharafUndertowHandler(SharafHandler.routes(routes))
    val engine = io.undertow.Undertow
      .builder()
      .addHttpListener(enginePort, "127.0.0.1")
      .setHandler(new io.undertow.server.handlers.BlockingHandler(exchange => {
        Seq("X-Keep", "X-Remove", "X-Empty", "X-Replace").foreach { name =>
          exchange.getResponseHeaders.put(new io.undertow.util.HttpString(name), "engine")
        }
        handler.handleRequest(exchange)
      }))
      .build()
    engine.start()
    try {
      val response = quickRequest.get(uri"http://127.0.0.1:$enginePort/").send()
      assertEquals(response.body, "hello")
      assertEquals(response.headers("X-Keep"), Seq("engine"))
      assertEquals(response.headers("X-Remove"), Seq.empty)
      assertEquals(response.headers("X-Empty"), Seq.empty)
      assertEquals(response.headers("X-Replace").sorted, Seq("first", "second"))
    } finally engine.stop()
  }

}
