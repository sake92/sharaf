package ba.sake.sharaf

import ba.sake.formson.FormValue
import ba.sake.querson.QueryStringMap
import ba.sake.sharaf.exceptions.UnsupportedMediaTypeException

class RequestDefaultsTest extends munit.FunSuite {
  private def request(requestHeaders: Map[HttpString, Seq[String]], body: String): Request = new Request {
    def headers: Map[HttpString, Seq[String]] = requestHeaders
    def queryParamsRaw: QueryStringMap = Map.empty
    def bodyString: String = body
  }

  test("default cookies preserve repeated cookie headers and values") {
    val req = request(Map(HttpString("cookie") -> Seq("a=one; a=two", "token=a=b")), "")
    assertEquals(req.cookies.map(c => c.name -> c.value), Seq("a" -> "one", "a" -> "two", "token" -> "a=b"))
  }

  test("default form parses URL-encoded body and rejects other media types") {
    val req = request(
      Map(HttpString("Content-Type") -> Seq("Application/X-Www-Form-Urlencoded; charset=UTF-8")),
      "a=1&a=two+words"
    )
    assertEquals(req.bodyFormRaw("a"), Seq(FormValue.Str("1"), FormValue.Str("two words")))
    intercept[UnsupportedMediaTypeException](request(Map.empty, "a=1").bodyFormRaw)
  }
}
