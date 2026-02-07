package ba.sake.sharaf.handlers

import java.nio.file.Paths
import sttp.client4.quick.*
import sttp.model.{HeaderNames, StatusCode}
import ba.sake.sharaf.*

abstract class AbstractFilesHandlerTest extends munit.FunSuite {

  def port: Int
  def testResourcesDir = Paths.get(sys.env("MILL_TEST_RESOURCE_DIR"))

  // Abstract method to start the server - implementations must provide this
  def startServer(): Unit

  // Abstract method to stop the server - implementations must provide this
  def stopServer(): Unit

  val filesHandler = SharafHandler.files(
    testResourcesDir.resolve("myfiles")
  )
  val routesHandler = SharafHandler.routes(
    Routes { case GET -> Path("hello") =>
      Response.withBody("Hello World!")
    },
    filesHandler
  )

  override def beforeAll(): Unit = startServer()

  override def afterAll(): Unit = stopServer()

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