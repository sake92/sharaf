package ba.sake.sharaf.undertow.handlers

import io.undertow.{Handlers, Undertow}
import sttp.model.*
import sttp.client4.quick.*
import ba.sake.formson.FormDataRW
import ba.sake.querson.QueryStringRW
import ba.sake.tupson.JsonRW
import ba.sake.validson.Validator
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.utils.*
import ba.sake.sharaf.undertow.SharafUndertowHandler
import io.undertow.server.handlers.BlockingHandler

class ErrorHandlerTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
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
    case GET -> Path() =>
      Response.withBody("OK")
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(
      Handlers
        .path()
        .addPrefixPath(
          "default",
          BlockingHandler(
            SharafUndertowHandler(
              SharafHandler.exceptions(SharafHandler.routes(routes))
            )
          )
        )
        .addPrefixPath(
          "json",
          BlockingHandler(
            SharafUndertowHandler(
              SharafHandler.exceptions(
                SharafHandler.routes(routes),
                ExceptionMapper.json
              )
            )
          )
        )
    )
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  // default (plain string) error mapper
  test("Default error mapper handles query parsing failure") {
    val res = quickRequest.get(uri"${baseUrl}/default/query").send()
    assertEquals(res.code, StatusCode.BadRequest)
    assertEquals(res.body, "Query string parsing error: Key 'name' is missing")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain; charset=utf-8"))
  }
  test("Default error mapper handles query validation failure") {
    val res = quickRequest.get(uri"${baseUrl}/default/query?name=").send()
    assertEquals(res.code, StatusCode.UnprocessableEntity)
    assertEquals(res.body, "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain; charset=utf-8"))
  }

  test("Default error mapper handles form parsing failure") {
    val res = quickRequest.post(uri"${baseUrl}/default/form").multipartBody(multipart("bla", "")).send()
    assertEquals(res.code, StatusCode.BadRequest)
    assertEquals(res.body, "Form parsing error: Key 'name' is missing")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain; charset=utf-8"))
  }
  test("Default error mapper handles form validation failure") {
    val res = quickRequest.post(uri"${baseUrl}/default/form").multipartBody(TestForm("").toSttpMultipart()).send()
    assertEquals(res.code, StatusCode.UnprocessableEntity)
    assertEquals(res.body, "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain; charset=utf-8"))
  }

  test("Default error mapper handles JSON parsing failure") {
    val res = quickRequest.post(uri"${baseUrl}/default/json").body("").send()
    assertEquals(res.code, StatusCode.BadRequest)
    assertEquals(res.body, "JSON parsing exception")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain; charset=utf-8"))
  }
  test("Default error mapper handles JSON validation failure") {
    val res = quickRequest.post(uri"${baseUrl}/default/json").body(""" { "name": "" } """).send()
    assertEquals(res.code, StatusCode.UnprocessableEntity)
    assertEquals(res.body, "Validation errors: [ValidationError($.name,must be >= 3,)]")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain; charset=utf-8"))
  }

  // JSON error mapper
  test("JSON error mapper handles query parsing failure") {
    val res = quickRequest.get(uri"${baseUrl}/json/query").send()
    assertEquals(res.code, StatusCode.BadRequest)
    assertEquals(
      res.body,
      """{"instance":null,"invalidArguments":[{"reason":"is missing","path":"name","value":null}],"detail":"","type":null,"title":"Invalid query parameters","status":400}"""
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
  }
  test("JSON error mapper handles query validation failure") {
    val res = quickRequest.get(uri"${baseUrl}/json/query?name=").send()
    assertEquals(res.code, StatusCode.UnprocessableEntity)
    assertEquals(
      res.body,
      """{"instance":null,"invalidArguments":[{"reason":"must be >= 3","path":"$.name","value":""}],"detail":"","type":null,"title":"Validation errors","status":422}"""
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
  }

  test("JSON error mapper handles form parsing failure") {
    val res = quickRequest.post(uri"${baseUrl}/json/form").multipartBody(multipart("bla", "")).send()
    assertEquals(res.code, StatusCode.BadRequest)
    assertEquals(
      res.body,
      """{"instance":null,"invalidArguments":[],"detail":"Form parsing error: Key 'name' is missing","type":null,"title":"Form parsing error","status":400}"""
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
  }
  test("JSON error mapper handles form validation failure") {
    val res = quickRequest.post(uri"${baseUrl}/json/form").multipartBody(TestForm("").toSttpMultipart()).send()
    assertEquals(res.code, StatusCode.UnprocessableEntity)
    assertEquals(
      res.body,
      """{"instance":null,"invalidArguments":[{"reason":"must be >= 3","path":"$.name","value":""}],"detail":"","type":null,"title":"Validation errors","status":422}"""
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
  }

  test("JSON error mapper handles JSON parsing failure") {
    val res = quickRequest.post(uri"${baseUrl}/json/json").body("").send()
    assertEquals(res.code, StatusCode.BadRequest)
    assertEquals(
      res.body,
      """{"instance":null,"invalidArguments":[],"detail":"JSON parsing exception","type":null,"title":"JSON parsing error","status":400}"""
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
  }
  test("JSON error mapper handles JSON validation failure") {
    val res = quickRequest.post(uri"${baseUrl}/json/json").body(""" { "name": "" } """).send()
    assertEquals(res.code, StatusCode.UnprocessableEntity)
    assertEquals(
      res.body,
      """{"instance":null,"invalidArguments":[{"reason":"must be >= 3","path":"$.name","value":""}],"detail":"","type":null,"title":"Validation errors","status":422}"""
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
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
