package ba.sake.sharaf.helidon.handlers

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.helidon.HelidonSharafServer
import ba.sake.sharaf.utils.NetworkUtils
import sttp.client4.quick.*
import sttp.model.*

class RequestFormsTest extends munit.FunSuite {
  case class Form(name: String, tag: Seq[String]) derives FormDataRW

  private val port = NetworkUtils.getFreePort()
  private val baseUrl = s"http://localhost:$port"
  private val routes = Routes {
    case POST -> Path("form") =>
      val form = Request.current.bodyForm[Form]
      Response.withBody(s"${form.name}:${form.tag.mkString(",")}")
    case GET -> Path("cookie") =>
      Response.withBody(Request.current.cookies.map(c => s"${c.name}=${c.value}").mkString(","))
  }
  private val server = HelidonSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()
  override def afterAll(): Unit = server.stop()

  test("URL encoded form values are decoded and repeated fields are retained") {
    val res = quickRequest.post(uri"$baseUrl/form")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .body("name=Jane+Doe&tag=a%2Bb&tag=two")
      .send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "Jane Doe:a+b,two")
  }

  test("request cookies are available to routes") {
    val res = quickRequest.get(uri"$baseUrl/cookie").header("Cookie", "first=one; second=two=three").send()
    assertEquals(res.body, "first=one,second=two=three")
  }
}
