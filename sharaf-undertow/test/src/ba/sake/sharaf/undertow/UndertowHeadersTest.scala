package ba.sake.sharaf.undertow

import sttp.client4.quick.*
import ba.sake.sharaf.*

class UndertowHeadersTest extends munit.FunSuite {

  test("response updates preserve and remove headers set by an earlier Undertow handler") {
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
      .addHttpListener(0, "127.0.0.1")
      .setHandler(new io.undertow.server.handlers.BlockingHandler(exchange => {
        Seq("X-Keep", "X-Remove", "X-Empty", "X-Replace").foreach { name =>
          exchange.getResponseHeaders.put(new io.undertow.util.HttpString(name), "engine")
        }
        handler.handleRequest(exchange)
      }))
      .build()
    engine.start()
    val port = engine.getListenerInfo.get(0).getAddress.asInstanceOf[java.net.InetSocketAddress].getPort
    try {
      val response = quickRequest.get(uri"http://127.0.0.1:$port/").send()
      assertEquals(response.body, "hello")
      assertEquals(response.headers("X-Keep"), Seq("engine"))
      assertEquals(response.headers("X-Remove"), Seq.empty)
      assertEquals(response.headers("X-Empty"), Seq.empty)
      assertEquals(response.headers("X-Replace").sorted, Seq("first", "second"))
    } finally engine.stop()
  }

  test("mounted handler splits encoded path segments before decoding") {
    val routes = Routes { case GET -> Path("a/b") => Response.withBody("mounted") }
    val handler = SharafUndertowHandler(SharafHandler.routes(routes))
    val engine = io.undertow.Undertow
      .builder()
      .addHttpListener(0, "127.0.0.1")
      .setHandler(
        io.undertow.Handlers.path().addPrefixPath("/mount", new io.undertow.server.handlers.BlockingHandler(handler))
      )
      .build()
    engine.start()
    val port = engine.getListenerInfo.get(0).getAddress.asInstanceOf[java.net.InetSocketAddress].getPort
    try {
      val response = quickRequest
        .httpVersion(sttp.model.HttpVersion.HTTP_1_1)
        .get(sttp.model.Uri.unsafeParse(s"http://127.0.0.1:$port/mount/a%2Fb"))
        .send()
      assertEquals(response.code.code, 200)
      assertEquals(response.body, "mounted")
    } finally engine.stop()
  }

}
