package ba.sake.sharaf.undertow

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import sttp.model.*
import sttp.client4.quick.*
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer
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

}
