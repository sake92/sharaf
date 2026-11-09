package ba.sake.sharaf

import ba.sake.formson.FormValue
import ba.sake.sharaf.exceptions.BadRequestException

class UrlEncodedTest extends munit.FunSuite {
  test("raw pairs preserve repetition, empty names and values, and insertion order") {
    val raw = "&b=first&a=&b=second&missing&=empty-name&&equals=a=b&"
    val form = UrlEncoded.form(raw)
    assertEquals(form.keys.toSeq, Seq("b", "a", "missing", "", "equals"))
    assertEquals(form("b"), Seq(FormValue.Str("first"), FormValue.Str("second")))
    assertEquals(form("a"), Seq(FormValue.Str("")))
    assertEquals(form("missing"), Seq(FormValue.Str("")))
    assertEquals(form(""), Seq(FormValue.Str("empty-name")))
    assertEquals(form("equals"), Seq(FormValue.Str("a=b")))
    assertEquals(
      UrlEncoded.query(raw),
      Map(
        "b" -> Seq("first", "second"),
        "a" -> Seq(""),
        "missing" -> Seq(""),
        "" -> Seq("empty-name"),
        "equals" -> Seq("a=b")
      )
    )
  }

  test("split before decoding names and values exactly once") {
    val raw = "a%26b=x%3Dy%26z&plus=one+two&plus=%2B&unicode=%C4%87&once=%252F&%253D=%2526"
    val expected = Map(
      "a&b" -> Seq("x=y&z"),
      "plus" -> Seq("one two", "+"),
      "unicode" -> Seq("ć"),
      "once" -> Seq("%2F"),
      "%3D" -> Seq("%26")
    )
    assertEquals(UrlEncoded.query(raw), expected)
    val formStrings = UrlEncoded.form(raw).map { (key, values) =>
      key -> values.collect { case FormValue.Str(value) => value }
    }
    assertEquals(formStrings.toMap, expected)
  }

  test("empty input and separator entries have no fields") {
    for raw <- Seq("", "&", "&&&") do {
      assertEquals(UrlEncoded.query(raw), Map.empty[String, Seq[String]])
      assert(UrlEncoded.form(raw).isEmpty)
    }
    assertEquals(UrlEncoded.query("="), Map("" -> Seq("")))
  }

  test("malformed percent escapes in names or values are bad requests") {
    for raw <- Seq("x=%", "x=%0", "x=%ZZ", "%GG=x", "x=%C4%") do {
      intercept[BadRequestException](UrlEncoded.query(raw))
      intercept[BadRequestException](UrlEncoded.form(raw))
    }
  }

  test("URLDecoder replaces malformed encoded UTF-8 bytes") {
    for encoded <- Seq("%FF", "%C0", "%C4") do assertEquals(UrlEncoded.query(s"x=$encoded"), Map("x" -> Seq("�")))
    assertEquals(UrlEncoded.query("x=%C0%AF"), Map("x" -> Seq("��")))
    assertEquals(UrlEncoded.query("x=%ED%A0%80"), Map("x" -> Seq("�")))
    assertEquals(UrlEncoded.query("x=%EF%BF%BD"), Map("x" -> Seq("�")))
    assertEquals(UrlEncoded.query("x=é+%FF%2B%C4%87+%C4"), Map("x" -> Seq("é �+ć �")))
  }

}
