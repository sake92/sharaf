package ba.sake.sharaf.jdkhttp

import scala.concurrent.duration.*
import sttp.client4.{DefaultSyncBackend, quickRequest, UriContext}
import ba.sake.sharaf.*
import ba.sake.sharaf.utils.NetworkUtils
import ba.sake.tupson.*

class JdkHttpServerTest extends munit.FunSuite {

  private case class Greeting(message: String) derives JsonRW

  test("real HTTP requests support text, JSON, invalid input and missing routes") {
    val port = NetworkUtils.getFreePort()
    val baseUrl = s"http://127.0.0.1:$port"
    val routes = Routes {
      case GET -> Path("hello") => Response.withBody("Hello Native! čćž")
      case GET -> Path("query") => Response.withBody(Request.current.queryParamsRaw("name").mkString(","))
      case POST -> Path("form") =>
        val values = Request.current.bodyFormRaw("name").collect { case ba.sake.formson.FormValue.Str(value) => value }
        Response.withBody(values.mkString(","))
      case POST -> Path("json") => Response.withBody(Request.current.bodyJson[Greeting])
    }
    val handler = SharafHandler.exceptions(SharafHandler.routes(routes), ExceptionMapper.json)
    val server = JdkHttpServerSharafServer("127.0.0.1", port, handler)
    val backend = DefaultSyncBackend()
    server.start()
    try {
      val text = quickRequest.get(uri"$baseUrl/hello").readTimeout(5.seconds).send(backend)
      assertEquals(text.code.code, 200)
      assertEquals(text.body, "Hello Native! čćž")
      assertEquals(text.header("Content-Type"), Some("text/plain; charset=utf-8"))

      val query =
        quickRequest.get(uri"$baseUrl/query?name=hello%20world&name=%C4%8D").readTimeout(5.seconds).send(backend)
      assertEquals(query.code.code, 200)
      assertEquals(query.body, "hello world,č")

      val form = quickRequest
        .post(uri"$baseUrl/form")
        .contentType("application/x-www-form-urlencoded")
        .body("name=hello+world&name=%C4%8D")
        .readTimeout(5.seconds)
        .send(backend)
      assertEquals(form.code.code, 200)
      assertEquals(form.body, "hello world,č")

      val greeting = Greeting("Hello Scala Native! čćž")
      val json = quickRequest.post(uri"$baseUrl/json").body(greeting.toJson).readTimeout(5.seconds).send(backend)
      assertEquals(json.code.code, 200)
      assertEquals(json.body.parseJson[Greeting], greeting)
      assertEquals(json.header("Content-Type"), Some("application/json; charset=utf-8"))

      val invalid = quickRequest.post(uri"$baseUrl/json").body("{}").readTimeout(5.seconds).send(backend)
      assertEquals(invalid.code.code, 400)

      val missing = quickRequest.get(uri"$baseUrl/missing").readTimeout(5.seconds).send(backend)
      assertEquals(missing.code.code, 404)
    } finally {
      backend.close()
      server.stop()
    }
  }
  for ((label, mapper) <- Seq("default" -> ExceptionMapper.default, "json" -> ExceptionMapper.json)) {
    test(s"$label mapper handles raw queries and URL-encoded form errors") {
      val port = NetworkUtils.getFreePort()
      val routes = Routes {
        case GET -> Path("query") => Response.withBody(Request.current.queryParamsRaw("name").mkString("|"))
        case POST -> Path("form") =>
          val form = Request.current.bodyFormRaw
          val values = form("name").collect { case ba.sake.formson.FormValue.Str(value) => value }
          Response.withBody(form.keys.mkString(",") + ":" + values.mkString("|"))
      }
      val handler = SharafHandler.exceptions(SharafHandler.routes(routes), mapper)
      val server = JdkHttpServerSharafServer("127.0.0.1", port, handler)
      val backend = sttp.client4.DefaultSyncBackend()
      server.start()
      try {
        val base = s"http://127.0.0.1:$port"
        val queryUri = sttp.model.Uri.unsafeParse(base + "/query?name=a%26b%3Dc&name=%2B&name=%252F&name=%FF")
        val query = quickRequest.get(queryUri).send(backend)
        assertEquals(query.code.code, 200)
        assertEquals(query.body, "a&b=c|+|%2F|�")
        val form = quickRequest
          .post(uri"$base/form")
          .contentType("Application/X-WWW-Form-Urlencoded; charset=UTF-8")
          .body("&name=one+two&other=x&name=%2B&name=&name=%252F&&")
          .send(backend)
        assertEquals(form.code.code, 200)
        assertEquals(form.body, "name,other:one two|+||%2F")
        for (
          scenario <- Seq(
            (contentType = "application/x-www-form-urlencoded", body = "name=%ZZ", status = 400),
            (contentType = "text/plain", body = "name=x", status = 415),
            (contentType = "multipart/form-data; boundary=x", body = "name=x", status = 415),
            (contentType = "application/x-www-form-urlencodedevil", body = "name=x", status = 415)
          )
        ) {
          val response =
            quickRequest.post(uri"$base/form").contentType(scenario.contentType).body(scenario.body).send(backend)
          assertEquals(response.code.code, scenario.status)
          if label == "json" then {
            val problem = response.body.parseJson[ba.sake.sharaf.exceptions.ProblemDetails]
            assertEquals(problem.status, scenario.status)
          }
        }
      } finally {
        backend.close()
        server.stop()
      }
    }
  }

}
