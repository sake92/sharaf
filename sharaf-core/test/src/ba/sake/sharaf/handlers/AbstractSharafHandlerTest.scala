package ba.sake.sharaf.handlers

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils

abstract class AbstractSharafHandlerTest extends munit.FunSuite {

  val port: Int = NetworkUtils.getFreePort()
  def baseUrl: String = s"http://localhost:${port}"

  def startServer(): Unit
  def stopServer(): Unit

  override def beforeAll(): Unit = startServer()
  override def afterAll(): Unit = stopServer()

  val routes = Routes {
    case GET -> Path("hello") => Response.withBody("hello")
    case GET -> Path("query") => Response.withBody(Request.current.queryParamsRaw("tag").mkString("|"))
    case POST -> Path("form") =>
      val values = Request.current.bodyFormRaw("tag").collect { case ba.sake.formson.FormValue.Str(value) => value }
      Response.withBody(values.mkString("|"))
  }

  test("/does-not-exist returns a 404") {
    val res = quickRequest.get(uri"${baseUrl}/does-not-exist").send()
    assertEquals(res.code, StatusCode.NotFound)
    assertEquals(res.body, "Not Found")
  }

  test("/hello returns a string") {
    val res = quickRequest.get(uri"${baseUrl}/hello").send()
    assertEquals(res.body, "hello")
  }
  test("query and URL-encoded form parsing retain fields and replace malformed UTF-8") {
    val encoded = "tag=a%26b%3Dc&tag=%2B&tag=&tag=%252F&tag=%FF"
    val expected = "a&b=c|+||%2F|�"
    val query = quickRequest.get(Uri.unsafeParse(s"$baseUrl/query?$encoded")).send()
    assertEquals(query.code, StatusCode.Ok)
    assertEquals(query.body, expected)
    val form = quickRequest
      .post(uri"$baseUrl/form")
      .contentType("application/x-www-form-urlencoded")
      .body(encoded)
      .send()
    assertEquals(form.code, StatusCode.Ok)
    assertEquals(form.body, expected)
  }

}
