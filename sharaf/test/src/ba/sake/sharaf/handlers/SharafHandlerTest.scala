package ba.sake.sharaf.handlers

import ba.sake.formson.FormDataRW
import ba.sake.querson.QueryStringRW
import io.undertow.{Handlers, Undertow}
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
    .setHandler(
      Handlers
        .path()
        .addPrefixPath("default", SharafHandler(routes))
        .addPrefixPath("json", SharafHandler(routes).withExceptionMapper(ExceptionMapper.json))
    )
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  // default (plain string) error mapper
  test("Default error mapper handles query parsing failure") {
    val res = requests.get(s"${baseUrl}/default/query", check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), "Query string parsing error: Key 'name' is missing")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }
  test("Default error mapper handles query validation failure") {
    val res = requests.get(s"${baseUrl}/default/query?name=", check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(res.text(), "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }

  test("Default error mapper handles form parsing failure") {
    val res =
      requests.post(s"${baseUrl}/default/form", data = requests.MultiPart(requests.MultiItem("bla", "")), check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), "Form parsing error: Key 'name' is missing")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }
  test("Default error mapper handles form validation failure") {
    val res = requests.post(s"${baseUrl}/default/form", data = TestForm("").toRequestsMultipart(), check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(res.text(), "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }

  test("Default error mapper handles JSON parsing failure") {
    val res = requests.post(s"${baseUrl}/default/json", data = "", check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), "JSON parsing exception")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }
  test("Default error mapper handles JSON validation failure") {
    val res = requests.post(s"${baseUrl}/default/json", data = """ { "name": "" } """, check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(res.text(), "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }

  // JSON error mapper
  test("JSON error mapper handles query parsing failure") {
    val res = requests.get(s"${baseUrl}/json/query", check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(
      res.text(),
      """{"instance":null,"invalidArguments":[{"reason":"is missing","path":"name","value":null}],"detail":"","type":null,"title":"Invalid query parameters","status":400}"""
    )
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/json"))
  }
  test("JSON error mapper handles query validation failure") {
    val res = requests.get(s"${baseUrl}/json/query?name=", check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(
      res.text(),
      """{"instance":null,"invalidArguments":[{"reason":"must be >= 3","path":"$.name","value":""}],"detail":"","type":null,"title":"Validation errors","status":400}"""
    )
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/json"))
  }

  test("JSON error mapper handles form parsing failure") {
    val res =
      requests.post(s"${baseUrl}/json/form", data = requests.MultiPart(requests.MultiItem("bla", "")), check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), """{"instance":null,"invalidArguments":[],"detail":"Form parsing error: Key 'name' is missing","type":null,"title":"Form parsing error","status":400}""")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/json"))
  }
  test("JSON error mapper handles form validation failure") {
    val res = requests.post(s"${baseUrl}/json/form", data = TestForm("").toRequestsMultipart(), check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(
      res.text(),
      """{"instance":null,"invalidArguments":[{"reason":"must be >= 3","path":"$.name","value":""}],"detail":"","type":null,"title":"Validation errors","status":400}"""
    )
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/json"))
  }

  test("JSON error mapper handles JSON parsing failure") {
    val res = requests.post(s"${baseUrl}/json/json", data = "", check = false)
    assertEquals(res.statusCode, StatusCodes.BAD_REQUEST)
    assertEquals(res.text(), """{"instance":null,"invalidArguments":[],"detail":"JSON parsing exception","type":null,"title":"JSON parsing error","status":400}""")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/json"))
  }
  test("JSON error mapper handles JSON validation failure") {
    val res = requests.post(s"${baseUrl}/json/json", data = """ { "name": "" } """, check = false)
    assertEquals(res.statusCode, StatusCodes.UNPROCESSABLE_ENTITY)
    assertEquals(
      res.text(),
      """{"instance":null,"invalidArguments":[{"reason":"must be >= 3","path":"$.name","value":""}],"detail":"","type":null,"title":"Validation errors","status":400}"""
    )
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/json"))
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
