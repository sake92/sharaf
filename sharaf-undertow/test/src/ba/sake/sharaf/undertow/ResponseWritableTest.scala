package ba.sake.sharaf.undertow

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.{*, given}
import ba.sake.sharaf.utils.NetworkUtils
import ba.sake.tupson.JsonRW

class ResponseWritableTest extends munit.FunSuite {

  val testFileResourceDir = Paths.get(sys.env("MILL_TEST_RESOURCE_DIR"))

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val routes = Routes {
    case GET -> Path("string") =>
      Response.withBody("a string")
    case GET -> Path("inputstream") =>
      val is = new java.io.ByteArrayInputStream("an inputstream".getBytes(StandardCharsets.UTF_8))
      Response.withBody(is)
    case GET -> Path("geny") =>
      val genyWritable: geny.Writable = "geny writable".getBytes(StandardCharsets.UTF_8)
      Response.withBody(genyWritable)
    case GET -> Path("imperative") =>
      Request.current
        .asInstanceOf[UndertowSharafRequest]
        .underlyingHttpServerExchange
        .getOutputStream
        .write("hello".getBytes(StandardCharsets.UTF_8))
      Response.default
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
    case GET -> Path("twirl", "html") =>
      Response.withBody(html"""
        <html>
          <head>
            <title>Twirl HTML</title>
          </head>
          <body>
            <h1>This is a Twirl HTML response</h1>
          </body>
        </html>
      """)
    case GET -> Path("twirl", "xml") =>
      Response.withBody(xml"""
        <?xml version="1.0" encoding="UTF-8"?>
        <note>
        <to>Tove</to>
        <from>Jani</from>
        <heading>Reminder</heading>
        <body>Don't forget me this weekend!</body>
        </note>
      """)
    case GET -> Path("scalatags", "doctype") =>
      import scalatags.Text.all.{title => _, *}
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

  val server = UndertowSharafServer("localhost", port, routes)

  override def beforeAll(): Unit = server.start()

  override def afterAll(): Unit = server.stop()

  test("Write response String") {
    val res = quickRequest.get(uri"${baseUrl}/string").send()
    assertEquals(res.body, "a string")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/plain; charset=utf-8"))
  }

  test("Write response InputStream") {
    val res = quickRequest.get(uri"${baseUrl}/inputstream").send()
    assertEquals(res.body, "an inputstream")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/octet-stream"))
  }

  test("Write response geny.Writable") {
    val res = quickRequest.get(uri"${baseUrl}/geny").send()
    assertEquals(res.body, "geny writable")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/octet-stream"))
  }

  test("Write response in an imperative way") {
    val res = quickRequest.get(uri"${baseUrl}/imperative").send()
    assertEquals(res.body, "hello")
  }

  test("Write response file") {
    val res = quickRequest.get(uri"${baseUrl}/file").send()
    assertEquals(res.body, "a text file")
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/octet-stream"))
    assertEquals(
      res.headers(HeaderNames.ContentDisposition),
      Seq(""" attachment; filename="text_file.txt" """.trim)
    )
  }

  test("Write response JSON") {
    val res = quickRequest.get(uri"${baseUrl}/json").send()
    assertEquals(res.body, """ {"name":"Meho","age":40} """.trim)
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/json; charset=utf-8"))
  }

  test("Write response twirl HTML") {
    val res = quickRequest.get(uri"${baseUrl}/twirl/html").send()
    assertEquals(
      res.body.trim,
      """
        <html>
          <head>
            <title>Twirl HTML</title>
          </head>
          <body>
            <h1>This is a Twirl HTML response</h1>
          </body>
        </html> """.trim
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/html; charset=utf-8"))
  }

  test("Write response twirl XML") {
    val res = quickRequest.get(uri"${baseUrl}/twirl/xml").send()
    assertEquals(
      res.body.trim,
      """ 
        <?xml version="1.0" encoding="UTF-8"?>
        <note>
        <to>Tove</to>
        <from>Jani</from>
        <heading>Reminder</heading>
        <body>Don't forget me this weekend!</body>
        </note>
     """.trim
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("application/xml; charset=utf-8"))
  }

  test("Write response scalatags Frag") {
    val res = quickRequest.get(uri"${baseUrl}/scalatags/frag").send()
    assertEquals(res.body, """ <div>this is a div</div> """.trim)
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/html; charset=utf-8"))
  }

  test("Write response scalatags doctype") {
    val res = quickRequest.get(uri"${baseUrl}/scalatags/doctype").send()
    assertEquals(
      res.body,
      """ <!DOCTYPE html><html><head><title>doctype title</title></head><body>this is doctype body</body></html> """.trim
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/html; charset=utf-8"))
  }

  test("Write response hepek HtmlPage") {
    val res = quickRequest.get(uri"${baseUrl}/hepek/htmlpage").send()
    assertEquals(
      res.body,
      """ <!DOCTYPE html><html lang="en"><head><meta charset="utf-8" /><meta http-equiv="X-UA-Compatible" content="ie=edge" /><meta name="viewport" content="width=device-width, initial-scale=1" /><meta name="generator" content="hepek" /><meta name="theme-color" content="#000" /><meta name="mobile-web-app-capable" content="yes" /><meta name="twitter:card" content="summary_large_image" /><title>changeme</title></head><body><div>this is body</div></body></html> """.trim
    )
    assertEquals(res.headers(HeaderNames.ContentType), Seq("text/html; charset=utf-8"))
  }

}
