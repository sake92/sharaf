package ba.sake.sharaf.handlers

import ba.sake.formson.FormDataRW
import ba.sake.querson.QueryStringRW
import io.undertow.Undertow
import io.undertow.util.Headers
import io.undertow.util.StatusCodes
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.utils.*
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator

class SharafHandlerTest extends munit.FunSuite {

  val port = getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("query") =>
      val qp = Request.current.queryParamsValidated[TestQuery]
      Response.withBody(qp.toString)
    case POST -> Path("form") =>
      val body = Request.current.bodyFormValidated[TestForm]
      Response.withBody(body.toString)
    case POST -> Path("json") =>
      val body = Request.current.bodyJsonValidated[TestJson]
      Response.withBody(body)
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Default error mapper handles query parsing failure") {
    val res = requests.get(s"${baseUrl}/query", check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), "Query string parsing error: Key 'name' is missing")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }
  test("Default error mapper handles query validation failure") {
    val res = requests.get(s"${baseUrl}/query?name=", check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(res.text(), "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }

  test("Default error mapper handles form parsing failure") {
    val res = requests.post(s"${baseUrl}/form", data = requests.MultiPart(requests.MultiItem("bla", "")), check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), "Form parsing error: Key 'name' is missing")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }
  test("Default error mapper handles form validation failure") {
    val res = requests.post(s"${baseUrl}/form", data = TestForm("").toRequestsMultipart(), check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(res.text(), "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }

  test("Default error mapper handles JSON parsing failure") {
    val res = requests.post(s"${baseUrl}/json", data = "", check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), "JSON parsing exception")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }
  test("Default error mapper handles JSON validation failure") {
    val res = requests.post(s"${baseUrl}/json", data = """ { "name": "" } """, check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(res.text(), "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }

  case class TestQuery(name: String) derives QueryStringRW
  object TestQuery {
    given Validator[TestQuery] = Validator.derived[TestQuery].minLength(_.name, 3)
  }

  case class TestForm(name: String) derives FormDataRW
  object TestForm {
    given Validator[TestForm] = Validator.derived[TestForm].minLength(_.name, 3)
  }

  case class TestJson(name: String) derives JsonRW
  object TestJson {
    given Validator[TestJson] = Validator.derived[TestJson].minLength(_.name, 3)
  }
}
