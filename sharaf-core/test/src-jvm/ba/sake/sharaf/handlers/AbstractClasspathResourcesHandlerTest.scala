package ba.sake.sharaf.handlers

import sttp.client4.quick.*
import sttp.model.{HeaderNames, StatusCode}
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

abstract class AbstractClasspathResourcesHandlerTest extends munit.FunSuite {

  val port: Int = NetworkUtils.getFreePort()
  def baseUrl: String = s"http://localhost:$port"

  def startServer(): Unit
  def stopServer(): Unit

  override def beforeAll(): Unit = startServer()
  override def afterAll(): Unit = stopServer()

  val routesHandler = SharafHandler.routes(
    Routes { case GET -> Path("hello") =>
      Response.withBody("Hello World!")
    }
  )
  val classpathResourcesHandler = SharafHandler.classpathResources("myfiles", routesHandler)

  test("GET text_file.txt should work") {
    val res = quickRequest.get(uri"$baseUrl/text_file.txt").send()
    assertEquals(res.body, "a text file")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain"))
  }

  test("HEAD text_file.txt should return headers only") {
    val res = quickRequest.head(uri"$baseUrl/text_file.txt").send()
    assertEquals(res.body, "")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain"))
  }

  test("Unknown resource should fall through to routes") {
    val res = quickRequest.get(uri"$baseUrl/hello").send()
    assertEquals(res.body, "Hello World!")
  }

  test("Unknown resource with no matching route should return not found") {
    val res = quickRequest.get(uri"$baseUrl/missing.txt").send()
    assertEquals(res.code, StatusCode.NotFound)
  }

  test("Suspicious path should be rejected") {
    val res = quickRequest.get(uri"$baseUrl/../text_file.txt").send()
    assertEquals(res.code, StatusCode.Forbidden)
  }

  test("Matching ETag should return not modified") {
    val initial = quickRequest.get(uri"$baseUrl/text_file.txt").send()
    val etag = initial.header(HeaderNames.Etag).getOrElse(fail("Expected an ETag header"))

    val cached = quickRequest
      .get(uri"$baseUrl/text_file.txt")
      .header(HeaderNames.IfNoneMatch, etag)
      .send()

    assertEquals(cached.code, StatusCode.NotModified)
    assertEquals(cached.body, "")
    assertEquals(cached.header(HeaderNames.Etag), Some(etag))
  }
}
