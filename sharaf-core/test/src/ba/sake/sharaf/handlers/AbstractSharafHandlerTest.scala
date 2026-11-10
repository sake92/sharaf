package ba.sake.sharaf.handlers

import scala.concurrent.duration.*
import scala.util.Using
import java.net.{InetSocketAddress, URI}
import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}
import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.tupson.*
import ba.sake.formson.*

abstract class AbstractSharafHandlerTest extends munit.FunSuite with ServerFixture {
  override def munitTimeout: Duration = 60.seconds

  private case class Greeting(message: String) derives JsonRW
  private case class Form(name: String, tag: Seq[String]) derives FormDataRW

  private val routes = Routes {
    case GET -> Path("path", value) => Response.withBody(value)
    case GET -> Path("hello")       => Response.withBody("Hello Scala! čćž")
    case GET -> Path("query")       => Response.withBody(Request.current.queryParamsRaw("tag").mkString("|"))
    case POST -> Path("json")       => Response.withBody(Request.current.bodyJson[Greeting])
    case POST -> Path("typed-form") =>
      val form = Request.current.bodyForm[Form]
      Response.withBody(s"${form.name}:${form.tag.mkString(",")}")
    case POST -> Path("ordered-form") =>
      val form = Request.current.bodyFormRaw
      val values = form("name").collect { case FormValue.Str(value) => value }
      Response.withBody(form.keys.mkString(",") + ":" + values.mkString("|"))
    case GET -> Path("headers") => Response.withBody(Request.current.headers(HttpString("x-values")).mkString("|"))
    case POST -> Path("form") =>
      val values = Request.current.bodyFormRaw("tag").collect { case ba.sake.formson.FormValue.Str(value) => value }
      Response.withBody(values.mkString("|"))
    case GET -> Path("sse") =>
      val sender = SseSender()
      sender.send(ServerSentEvent.Message("hello"))
      sender.send(ServerSentEvent.Done())
      Response.withBody(sender)
  }

  test("unknown route returns 404") {
    withServer(routes, ExceptionMapper.default) { base =>
      val response = request.get(uri"$base/does-not-exist").send(httpBackend)
      assertEquals(response.code, StatusCode.NotFound)
      assertEquals(response.body, "Not Found")
    }
  }

  test("text response reaches the client") {
    withServer(routes, ExceptionMapper.default) { base =>
      val response = request.get(uri"$base/hello").send(httpBackend)
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "Hello Scala! čćž")
    }
  }

  test("JSON request bodies and responses retain Unicode") {
    withServer(routes, ExceptionMapper.json) { base =>
      val greeting = Greeting("Hello Scala! čćž")
      val response = request.post(uri"$base/json").body(greeting.toJson).send(httpBackend)
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body.parseJson[Greeting], greeting)
      assertEquals(response.header("Content-Type"), Some("application/json; charset=utf-8"))
      assertEquals(request.post(uri"$base/json").body("{}").send(httpBackend).code, StatusCode.BadRequest)
    }
  }

  test("typed forms retain repeated fields") {
    withServer(routes, ExceptionMapper.default) { base =>
      val response = request
        .post(uri"$base/typed-form")
        .contentType("application/x-www-form-urlencoded")
        .body("name=Jane+Doe&tag=a%2Bb&tag=two")
        .send(httpBackend)
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "Jane Doe:a+b,two")
    }
  }

  test("URL-encoded forms retain key order and ignore empty fields") {
    withServer(routes, ExceptionMapper.default) { base =>
      val response = request
        .post(uri"$base/ordered-form")
        .contentType("Application/X-WWW-Form-Urlencoded; charset=UTF-8")
        .body("&name=one+two&other=x&name=%2B&name=&name=%252F&&")
        .send(httpBackend)
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "name,other:one two|+||%2F")
    }
  }

  test("request header lookup is case insensitive and retains repeated values") {
    withServer(routes, ExceptionMapper.default) { base =>
      val response = request
        .get(uri"$base/headers")
        .header("X-Values", "one")
        .header("x-values", "two", sttp.client4.DuplicateHeaderBehavior.Add)
        .send(httpBackend)
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.body, "one|two")
    }
  }

  for source <- Seq("query", "form") do {
    test(s"$source decoding preserves repetition, empty values, plus and Unicode") {
      withServer(routes, ExceptionMapper.default) { base =>
        val encoded = "tag=a%26b%3Dc&tag=one+two&tag=%2B&tag=&tag=%252F&tag=%C4%87&tag=%FF"
        val response =
          if source == "query" then request.get(Uri.unsafeParse(s"$base/query?$encoded")).send(httpBackend)
          else
            request
              .post(uri"$base/form")
              .contentType("application/x-www-form-urlencoded; charset=UTF-8")
              .body(encoded)
              .send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body, "a&b=c|one two|+||%2F|ć|�")
      }
    }
  }

  for scenario <- Seq(
      (encoded = "a%2Fb", decoded = "a/b"),
      (encoded = "a+b", decoded = "a+b"),
      (encoded = "a%2Bb", decoded = "a+b"),
      (encoded = "%C4%87", decoded = "ć"),
      (encoded = "%252F", decoded = "%2F")
    )
  do
    test(s"path decodes exactly once: ${scenario.encoded}") {
      withServer(routes, ExceptionMapper.default) { base =>
        val response = request.get(Uri.unsafeParse(s"$base/path/${scenario.encoded}")).send(httpBackend)
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.body, scenario.decoded)
      }
    }

  for mapper <- Seq((name = "default", value = ExceptionMapper.default), (name = "json", value = ExceptionMapper.json))
  do {
    for contentType <- Seq("text/plain", "application/x-www-form-urlencodedevil") do {
      test(s"${mapper.name}: URL-encoded forms reject $contentType") {
        withServer(routes, mapper.value) { base =>
          val response = request.post(uri"$base/form").contentType(contentType).body("tag=value").send(httpBackend)
          assertEquals(response.code, StatusCode.UnsupportedMediaType)
          if mapper.name == "json" then
            assertEquals(response.body.parseJson[ba.sake.sharaf.exceptions.ProblemDetails].status, 415)
        }
      }
    }
  }

  test("SSE delivers events and completes") {
    withServer(routes, ExceptionMapper.default) { base =>
      val response = request.get(uri"$base/sse").send(httpBackend)
      assertEquals(response.code, StatusCode.Ok)
      assertEquals(response.header("Content-Type"), Some("text/event-stream"))
      assertEquals(response.body, "data: hello\n\nevent: stop\ndata:\n\n")
    }
  }

  private case class JsonItem(id: Int, text: String, tags: Seq[String]) derives JsonRW
  private case class JsonPayload(items: Seq[JsonItem], count: Int) derives JsonRW

  test("concurrent JSON responses retain large nested payloads") {
    val items = (0 until 50).map(i => JsonItem(i, "ćž\n\"\\" * 40, Seq("one", "two")))
    val expected = JsonPayload(items, 50)
    val jsonRoutes = Routes { case GET -> Path("json") => Response.withBody(expected) }
    withServer(jsonRoutes, ExceptionMapper.default) { base =>
      val pool = Executors.newFixedThreadPool(32)
      val finished = new CountDownLatch(32)
      val failures = new java.util.concurrent.ConcurrentLinkedQueue[Throwable]()
      val responses = new java.util.concurrent.ConcurrentLinkedQueue[(status: Int, payload: JsonPayload)]()
      try {
        for _ <- 0 until 32 do
          pool.execute(() => {
            try {
              val uri = URI.create(base)
              Using.resource(new HttpConnection(new InetSocketAddress(uri.getHost, uri.getPort))) { connection =>
                for _ <- 0 until 10 do {
                  val response = connection.request("GET", "/json")
                  responses.add((status = response.status, payload = response.body.parseJson[JsonPayload]))
                }
              }
            } catch {
              case scala.util.control.NonFatal(error) =>
                failures.add(error)
                ()
            } finally finished.countDown()
          })
        // Workers collect results; assertions run on the test thread after all requests complete.
        val allFinished = finished.await(45, TimeUnit.SECONDS)
        assert(allFinished, "concurrent requests did not finish")
        if !failures.isEmpty then throw failures.peek()
        assertEquals(responses.size(), 320)
        val iterator = responses.iterator()
        while iterator.hasNext do {
          val response = iterator.next()
          assertEquals(response.status, 200)
          assertEquals(response.payload, expected)
        }
      } finally {
        pool.shutdownNow()
        val terminated = pool.awaitTermination(5, TimeUnit.SECONDS)
        assert(terminated)
      }
    }
  }
}
