package ba.sake.sharaf.handlers

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.formson.FormDataRW
import ba.sake.querson.QueryStringRW
import ba.sake.tupson.*
import ba.sake.validson.Validator
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.ProblemDetails

abstract class AbstractErrorHandlerTest extends munit.FunSuite with ServerFixture {

  private def routes = Routes {
    case GET -> Path("query") =>
      val qp = Request.current.queryParamsValidated[TestQuery]
      Response.withBody(qp.toString)
    case POST -> Path("form") =>
      val body = Request.current.bodyFormValidated[TestForm]
      Response.withBody(body.toString)
    case POST -> Path("json") =>
      val body = Request.current.bodyJsonValidated[TestJson]
      Response.withBody(body)
    case GET -> Path("fail") => throw new IllegalArgumentException("private application error")
    case GET -> Path() =>
      Response.withBody("OK")
  }

  private val errors = Seq(
    (
      name = "query parsing",
      method = Method.GET,
      path = "query",
      body = "",
      status = StatusCode.BadRequest,
      text = "Query string parsing error: Key 'name' is missing",
      problem = ProblemDetails(
        400,
        "Invalid query parameters",
        invalidArguments = Seq(ProblemDetails.ArgumentProblem("name", "is missing", None))
      )
    ),
    (
      name = "query validation",
      method = Method.GET,
      path = "query?name=",
      body = "",
      status = StatusCode.UnprocessableEntity,
      text = "Validation errors: [ValidationError($.name,must be >= 3,)]",
      problem = ProblemDetails(
        422,
        "Validation errors",
        invalidArguments = Seq(ProblemDetails.ArgumentProblem("$.name", "must be >= 3", Some("")))
      )
    ),
    (
      name = "JSON parsing",
      method = Method.POST,
      path = "json",
      body = "",
      status = StatusCode.BadRequest,
      text = "JSON parsing exception",
      problem = ProblemDetails(400, "JSON parsing error", "JSON parsing exception")
    ),
    (
      name = "JSON validation",
      method = Method.POST,
      path = "json",
      body = """ { "name": "" } """,
      status = StatusCode.UnprocessableEntity,
      text = "Validation errors: [ValidationError($.name,must be >= 3,)]",
      problem = ProblemDetails(
        422,
        "Validation errors",
        invalidArguments = Seq(ProblemDetails.ArgumentProblem("$.name", "must be >= 3", Some("")))
      )
    ),
    (
      name = "URL-encoded form parsing",
      method = Method.POST,
      path = "form",
      body = "bla=",
      status = StatusCode.BadRequest,
      text = "Form parsing error: Key 'name' is missing",
      problem = ProblemDetails(
        400,
        "Form parsing errors",
        invalidArguments = Seq(ProblemDetails.ArgumentProblem("name", "is missing", None))
      )
    ),
    (
      name = "URL-encoded form validation",
      method = Method.POST,
      path = "form",
      body = "name=",
      status = StatusCode.UnprocessableEntity,
      text = "Validation errors: [ValidationError($.name,must be >= 3,)]",
      problem = ProblemDetails(
        422,
        "Validation errors",
        invalidArguments = Seq(ProblemDetails.ArgumentProblem("$.name", "must be >= 3", Some("")))
      )
    )
  )

  for mapper <- Seq((name = "default", value = ExceptionMapper.default), (name = "json", value = ExceptionMapper.json))
  do {
    for error <- errors do {
      test(s"${mapper.name} mapper handles ${error.name} failure") {
        withServer(routes, mapper.value) { base =>
          val contentType =
            if error.path == "form" then "application/x-www-form-urlencoded; charset=UTF-8"
            else "application/json"
          val prepared = request.method(error.method, Uri.unsafeParse(s"$base/${error.path}")).contentType(contentType)
          val response =
            if error.method == Method.GET then prepared.send(httpBackend)
            else prepared.body(error.body).send(httpBackend)
          assertEquals(response.code, error.status)
          val expectedType =
            if mapper.name == "json" then "application/json; charset=utf-8"
            else "text/plain; charset=utf-8"
          assertEquals(response.headers(HeaderNames.ContentType), Seq(expectedType))
          if mapper.name == "json" then assertEquals(response.body.parseJson[ProblemDetails], error.problem)
          else assertEquals(response.body, error.text)
        }
      }
    }

    test(s"${mapper.name} mapper rejects malformed URL-encoded escapes") {
      withServer(routes, mapper.value) { base =>
        for body <- Seq("name=%", "name=%0", "name=%ZZ") do {
          val response = request
            .post(uri"$base/form")
            .contentType("application/x-www-form-urlencoded")
            .body(body)
            .send(httpBackend)
          assertEquals(response.code, StatusCode.BadRequest)
          val expectedType =
            if mapper.name == "json" then "application/json; charset=utf-8"
            else "text/plain; charset=utf-8"
          assertEquals(response.headers(HeaderNames.ContentType), Seq(expectedType))
          if mapper.name == "json" then assertEquals(response.body.parseJson[ProblemDetails].status, 400)
        }
      }
    }

    test(s"${mapper.name} mapper accepts typed query and URL-encoded form values") {
      withServer(routes, mapper.value) { base =>
        for scenario <- Seq((encoded = "hello", decoded = "hello"), (encoded = "%FFabc", decoded = "�abc")) do {
          val query = request.get(Uri.unsafeParse(s"$base/query?name=${scenario.encoded}")).send(httpBackend)
          assertEquals(query.code, StatusCode.Ok)
          assertEquals(query.body, s"TestQuery(${scenario.decoded})")
          val form = request
            .post(uri"$base/form")
            .contentType("Application/X-WWW-Form-Urlencoded; charset=UTF-8")
            .body(s"name=${scenario.encoded}")
            .send(httpBackend)
          assertEquals(form.code, StatusCode.Ok)
          assertEquals(form.body, s"TestForm(${scenario.decoded})")
        }
      }
    }

    test(s"${mapper.name} mapper preserves server errors without leaking exception details") {
      withServer(routes, mapper.value) { base =>
        val response = request.get(uri"$base/fail").send(httpBackend)
        assertEquals(response.code, StatusCode.InternalServerError)
        assert(!response.body.contains("private application error"))
        if mapper.name == "json" then assertEquals(response.body.parseJson[ProblemDetails].status, 500)
        else assertEquals(response.body, "Server error")
        assertEquals(request.get(uri"$base/").send(httpBackend).body, "OK")
      }
    }
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
