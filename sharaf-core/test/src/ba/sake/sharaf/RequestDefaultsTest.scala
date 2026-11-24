package ba.sake.sharaf

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import ba.sake.formson.FormValue
import ba.sake.querson.QueryStringMap
import ba.sake.sharaf.exceptions.{PayloadTooLargeException, UnsupportedMediaTypeException}

class RequestDefaultsTest extends munit.FunSuite {
  private def request(requestHeaders: Map[HttpString, Seq[String]], body: String): Request = new Request {
    def headers: Map[HttpString, Seq[String]] = requestHeaders
    def queryParamsRaw: QueryStringMap = Map.empty
    override def bodyString: String = body
  }

  private def streamedRequest(contentType: String, input: ByteArrayInputStream): Request = new Request {
    def headers: Map[HttpString, Seq[String]] = Map(HttpString("Content-Type") -> Seq(contentType))
    def queryParamsRaw: QueryStringMap = Map.empty
    override def bodyInputStream: ByteArrayInputStream = input
  }

  test("default text reader decodes UTF-8 and caches the result") {
    val input = new ByteArrayInputStream("Hello čćž".getBytes(UTF_8))
    val req = streamedRequest("text/plain", input)
    assertEquals(req.bodyString, "Hello čćž")
    assertEquals(input.available(), 0)
    assertEquals(req.bodyString, "Hello čćž")
  }

  test("default form reader rejects oversized bodies before reading them in full") {
    val input = new ByteArrayInputStream(new Array[Byte](UrlEncoded.maxFormBytes + 16))
    val req = streamedRequest("Application/X-Www-Form-Urlencoded; charset=UTF-8", input)
    intercept[PayloadTooLargeException](req.bodyFormRaw)
    assertEquals(input.available(), 15)
  }

  test("a custom request can keep its text reader without implementing a binary reader") {
    val req = request(Map.empty, "text")
    assertEquals(req.bodyString, "text")
    intercept[UnsupportedOperationException](req.bodyInputStream)
  }

  test("default cookies preserve repeated cookie headers and values") {
    val req = request(Map(HttpString("cookie") -> Seq("a=one; a=two", "token=a=b")), "")
    assertEquals(req.cookies.map(c => c.name -> c.value), Seq("a" -> "one", "a" -> "two", "token" -> "a=b"))
  }

  test("default form caches decoded URL-encoded fields and rejects unsupported media types") {
    val req = request(
      Map(HttpString("Content-Type") -> Seq("Application/X-Www-Form-Urlencoded; charset=UTF-8")),
      "a=1&a=two+words"
    )
    val form = req.bodyFormRaw
    assertEquals(form("a"), Seq(FormValue.Str("1"), FormValue.Str("two words")))
    assert(req.bodyFormRaw eq form)
    intercept[UnsupportedMediaTypeException](request(Map.empty, "a=1").bodyFormRaw)
    val multipart = request(Map(HttpString("Content-Type") -> Seq("multipart/form-data; boundary=upload")), "")
    intercept[UnsupportedMediaTypeException](multipart.bodyFormRaw)
  }
}
