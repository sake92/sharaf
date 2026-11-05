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
}
