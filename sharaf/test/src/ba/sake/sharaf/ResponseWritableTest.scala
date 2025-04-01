package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import io.undertow.Undertow
import io.undertow.util.Headers
import ba.sake.sharaf.routing.*
import ba.sake.tupson.JsonRW

class ResponseWritableTest extends munit.FunSuite {

  val testFileResourceDir = Paths.get(sys.env("MILL_TEST_RESOURCE_DIR"))

  val port = utils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("string") =>
      Response.withBody("a string")
    case GET -> Path("inputstream") =>
      val is = new java.io.ByteArrayInputStream("an inputstream".getBytes(StandardCharsets.UTF_8))
      Response.withBody(is)
    case GET -> Path("geny") =>
      val genyReadable = requests.get.stream(s"${baseUrl}/inputstream")
      Response.withBody(genyReadable)
    case GET -> Path("imperative") =>
      Request.current.underlyingHttpServerExchange.getOutputStream.write("hello".getBytes(StandardCharsets.UTF_8))
      Response.withStatus(200)
    case GET -> Path("file") =>
      val file = testFileResourceDir.resolve("text_file.txt")
      Response.withBody(file)
    case GET -> Path("json") =>
      case class JsonCaseClass(name: String, age: Int) derives JsonRW
      val json = JsonCaseClass("Meho", 40)
      Response.withBody(json)
    case GET -> Path("scalatags", "frag") =>
      import scalatags.Text.all.*
      val res = div("this is a div")
      Response.withBody(res)
    case GET -> Path("scalatags", "doctype") =>
      import scalatags.Text.all.{title =>_, *}
      import scalatags.Text.tags2.title
      val res = doctype("html")(
        html(
          head(
            title("doctype title")
          ),
          body(
            "this is doctype body"
          )
        )
      )
      Response.withBody(res)
    case GET -> Path("hepek", "htmlpage") =>
      import scalatags.Text.all.*
      import ba.sake.hepek.html.HtmlPage
      val page = new HtmlPage {
        override def pageContent = div("this is body")
      }
      Response.withBody(page)
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(SharafHandler(routes))
    .build()

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Write response String") {
    val res = requests.get(s"${baseUrl}/string")
    assertEquals(res.text(), "a string")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/plain"))
  }

  test("Write response InputStream") {
    val res = requests.get(s"${baseUrl}/inputstream")
    assertEquals(res.text(), "an inputstream")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/octet-stream"))
  }

  test("Write response geny.Readable") {
    val res = requests.get(s"${baseUrl}/geny")
    assertEquals(res.text(), "an inputstream")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/octet-stream"))
  }

  test("Write response in an imperative way") {
    val res = requests.get(s"${baseUrl}/imperative")
    assertEquals(res.text(), "hello")
  }

  test("Write response file") {
    val res = requests.get(s"${baseUrl}/file")
    assertEquals(res.text(), "a text file")
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/octet-stream"))
    assertEquals(res.headers(Headers.CONTENT_DISPOSITION_STRING.toLowerCase), Seq(""" attachment; filename="text_file.txt" """.trim))
  }

  test("Write response JSON") {
    val res = requests.get(s"${baseUrl}/json")
    assertEquals(res.text(), """ {"name":"Meho","age":40} """.trim)
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("application/json"))
  }

  test("Write response scalatags Frag") {
    val res = requests.get(s"${baseUrl}/scalatags/frag")
    assertEquals(res.text(), """ <div>this is a div</div> """.trim)
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/html; charset=utf-8"))
  }

  test("Write response scalatags doctype") {
    val res = requests.get(s"${baseUrl}/scalatags/doctype")
    assertEquals(res.text(), """ <!DOCTYPE html><html><head><title>doctype title</title></head><body>this is doctype body</body></html> """.trim)
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/html; charset=utf-8"))
  }

  test("Write response hepek HtmlPage") {
    val res = requests.get(s"${baseUrl}/hepek/htmlpage")
    assertEquals(res.text(), """ <!DOCTYPE html><html lang="en"><head><meta charset="utf-8" /><meta http-equiv="X-UA-Compatible" content="ie=edge" /><meta name="viewport" content="width=device-width, initial-scale=1" /><meta name="generator" content="hepek" /><meta name="theme-color" content="#000" /><meta name="mobile-web-app-capable" content="yes" /><meta name="twitter:card" content="summary_large_image" /><title>changeme</title></head><body><div>this is body</div></body></html> """.trim)
    assertEquals(res.headers(Headers.CONTENT_TYPE_STRING.toLowerCase), Seq("text/html; charset=utf-8"))
  }
  
}
