package ba.sake.sharaf.handlers

import java.nio.file.Paths
import sttp.client4.quick.*
import sttp.model.HeaderNames
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils
import ba.sake.sharaf.undertow.UndertowSharafServer
import sttp.model.StatusCode

class FilesHandlerTest extends munit.FunSuite {

  val testResourcesDir = Paths.get(sys.env("MILL_TEST_RESOURCE_DIR"))

  val filesHandler = SharafHandler.files(
    testResourcesDir.resolve("myfiles")
  )
  val routesHandler = SharafHandler.routes(
    Routes { case GET -> Path("hello") =>
      Response.withBody("Hello World!")
    },
    filesHandler
  )
  val port = NetworkUtils.getFreePort()
  val server = UndertowSharafServer("localhost", port, SharafHandler.exceptions(routesHandler, ExceptionMapper.default))

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("GET text_file.txt should work") {
    val res = quickRequest.get(uri"http://localhost:${port}/text_file.txt").send()
    assertEquals(res.body, "a text file")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain"))
  }

  test("HEAD text_file.txt should return headers only") {
    val res = quickRequest.head(uri"http://localhost:${port}/text_file.txt").send()
    assertEquals(res.body, "")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain"))
  }

  test("POST text_file.txt should be rejected") {
    val res = quickRequest.post(uri"http://localhost:${port}/text_file.txt").send()
    assertEquals(res.code, StatusCode.MethodNotAllowed)
  }

  test("Suspicious path should be rejected") {
    val res = quickRequest.get(uri"http://localhost:${port}/../text_file.txt").send()
    assertEquals(res.code, StatusCode.Forbidden)
  }

}
