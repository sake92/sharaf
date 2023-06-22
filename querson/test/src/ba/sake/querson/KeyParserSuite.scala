package ba.sake.querson

class KeyParserSuite extends munit.FunSuite {

  test("KeyParser should parse key into subparts correctly") {
    assertEquals(parseKey("abc"), Seq("abc"))

    // sequences
    assertEquals(parseKey("abc[0]"), Seq("abc", "0"))
    assertEquals(parseKey("abc[]"), Seq("abc", ""))

    // keys
    assertEquals(parseKey("abc[def]"), Seq("abc", "def"))
    assertEquals(parseKey("abc.def"), Seq("abc", "def"))

    // bracket key syntax
    assertEquals(parseKey("abc[def][ghi]"), Seq("abc", "def", "ghi"))
    assertEquals(parseKey("abc[0][ghi]"), Seq("abc", "0", "ghi"))
    assertEquals(parseKey("abc[def][0]"), Seq("abc", "def", "0"))

    // dot key syntax
    assertEquals(parseKey("abc.def.ghi"), Seq("abc", "def", "ghi"))
    assertEquals(parseKey("abc[0].ghi"), Seq("abc", "0", "ghi"))
    assertEquals(parseKey("abc.def[0]"), Seq("abc", "def", "0"))

    // mixed key syntax
    assertEquals(parseKey("abc[def].ghi"), Seq("abc", "def", "ghi"))
  }

  test("KeyParser should fail parse wrongly formatted key") {

    Seq("", "  ", "[", "]", ".", "abc[", "abc[0]ghi]", "abc[0.ghi]").foreach { wrongKey =>
      intercept[QuersonException] {
        parseKey(wrongKey)
      }
    }
  }

  private def parseKey(key: String): Seq[String] =
    KeyParser(key).parse()

}
