package ba.sake.sharaf.undertow

import java.nio.charset.StandardCharsets
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

class UndertowExchangeWriteTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes { case GET -> Path("imperative") =>
    Request.current
      .asInstanceOf[UndertowSharafRequest]
      .underlyingHttpServerExchange
      .getOutputStream
      .write("hello".getBytes(StandardCharsets.UTF_8))
    Response.default
  }

  val server = UndertowSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Write response in an imperative way") {
    val res = quickRequest.get(uri"${baseUrl}/imperative").send()
    assertEquals(res.body, "hello")
  }

}
