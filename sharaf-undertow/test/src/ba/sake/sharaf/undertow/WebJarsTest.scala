package ba.sake.sharaf.undertow

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.utils.*
import ba.sake.sharaf.exceptions.NotFoundException

class WebJarsTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"
  val routes = Routes { case GET -> Path() =>
    Response.withBody("WebJars!")
  }

  // let SharafUndertowHandler do its thing.. for now!
  // TODO wont be needed once Sharaf handles classpath resources directly
  val customEm: ExceptionMapper = {
    case e: NotFoundException =>
      throw e
    case e => ExceptionMapper.default(e)
  }
  val nfHandler: SharafHandler = _ => throw NotFoundException("Resource not found")

  val server = UndertowSharafServer("localhost", port, routes, exceptionMapper = customEm, notFoundHandler = nfHandler)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("WebJars should work") {
    val res = quickRequest.get(uri"${baseUrl}/jquery/3.7.1/jquery.js").send()
    assert(res.body.length > 100)
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/javascript"))
  }
}
