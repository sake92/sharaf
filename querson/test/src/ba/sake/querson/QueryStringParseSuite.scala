package ba.sake.querson

import java.util.UUID

class QueryStringParseSuite extends munit.FunSuite {

  val uuid = UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")

  test("parseQueryStringMap should parse simple key/values") {
    Seq[(QueryStringMap, QuerySimple)](
      (
        Map(
          "str" -> Seq("text", "this_is_ignored"),
          "int" -> Seq("42"),
          "uuid" -> Seq(uuid.toString)
        ),
        QuerySimple("text", 42, uuid)
      )
    ).foreach { case (qsMap, expected) =>
      val res = qsMap.parseQueryStringMap[QuerySimple]
      assertEquals(res, expected)
    }
  }

  test("parseQueryStringMap should parse singleton-cases enum") {
    Seq[(QueryStringMap, QueryEnum)](
      (Map("color" -> Seq("Red")), QueryEnum(Color.Red))
    ).foreach { case (qsMap, expected) =>
      val res = qsMap.parseQueryStringMap[QueryEnum]
      assertEquals(res, expected)
    }
  }

  test("parseQueryStringMap should parse sequence") {
    Seq[(QueryStringMap, QuerySeq)](
      (Map(), QuerySeq(Seq())),
      (Map("a" -> Seq()), QuerySeq(Seq())),
      (Map("a" -> Seq("")), QuerySeq(Seq(""))),
      (Map("a" -> Seq("a1")), QuerySeq(Seq("a1"))),
      (Map("a" -> Seq("a1", "a2")), QuerySeq(Seq("a1", "a2"))),
      (Map("a[]" -> Seq("a1", "a2")), QuerySeq(Seq("a1", "a2"))),
      (
        Map(
          "a[3]" -> Seq("a3"),
          "a" -> Seq("a0", "a00"),
          "a[]" -> Seq("a0_1", "a0_11"),
          "a[1]" -> Seq("a1")
        ),
        QuerySeq(Seq("a0", "a00", "a0_1", "a0_11", "a1", "a3")) // sorted nicely
      )
    ).foreach { case (qsMap, expected) =>
      val res = qsMap.parseQueryStringMap[QuerySeq]
      assertEquals(res, expected)
    }
  }

  test("parseQueryStringMap should parse sequence of sequences") {
    Seq[(QueryStringMap, QuerySeqSeq)](
      (Map(), QuerySeqSeq(Seq())),
      (Map("a" -> Seq()), QuerySeqSeq(Seq())),
      (Map("a[][]" -> Seq("")), QuerySeqSeq(Seq(Seq("")))),
      (Map("a[][0]" -> Seq("a1")), QuerySeqSeq(Seq(Seq("a1"))))
      // TODO fix
      /*(
        Map(
          "a[1][3]" -> Seq("a13"),
          "a[2][3]" -> Seq("a23"),
          "a[][5]" -> Seq("a05"),
          "a[1][1]" -> Seq("a11"),
          "a[0][2]" -> Seq("a02")
        ),
        QuerySeqSeq(
          Seq(
            List("a02", "a05"),
            List("a11", "a13"),
            List("a23")
          )
        ) // sorted nicely

      ) */
    ).foreach { case (qsMap, expected) =>
      val res = qsMap.parseQueryStringMap[QuerySeqSeq]
      assertEquals(res, expected)
    }
  }

  test("parseQueryStringMap should parse nested fields") {
    Seq[(QueryStringMap, QueryNested)](
      (
        Map(
          "search" -> Seq("text", "this_is_ignored"),
          "p.number" -> Seq("3"),
          "p.size" -> Seq("50")
        ),
        QueryNested("text", Page(3, 50))
      ),
      (
        Map(
          "search" -> Seq("text", "this_is_ignored"),
          "p[number]" -> Seq("3"),
          "p[size]" -> Seq("50")
        ),
        QueryNested("text", Page(3, 50))
      )
    ).foreach { case (qsMap, expected) =>
      val res = qsMap.parseQueryStringMap[QueryNested]
      assertEquals(res, expected)
    }
  }

  test("parseQueryStringMap should parse falling back to defaults") {
    Seq[(QueryStringMap, QueryDefaults)](
      (
        Map(),
        QueryDefaults("default", None, Seq())
      ),
      (
        Map("q" -> Seq("q1"), "opt" -> Seq("optValue"), "seq" -> Seq("seq1", "seq2")),
        QueryDefaults("q1", Some("optValue"), Seq("seq1", "seq2"))
      )
    ).foreach { case (qsMap, expected) =>
      val res = qsMap.parseQueryStringMap[QueryDefaults]
      assertEquals(res, expected)
    }

    Seq[(QueryStringMap, QueryNestedDefaults)](
      (
        Map(),
        QueryNestedDefaults("default", Page(0, 10))
      ),
      (
        Map("search" -> Seq("q1"), "p.number" -> Seq("3"), "p.size" -> Seq("50")),
        QueryNestedDefaults("q1", Page(3, 50))
      )
    ).foreach { case (qsMap, expected) =>
      val res = qsMap.parseQueryStringMap[QueryNestedDefaults]
      assertEquals(res, expected)
    }

  }

  test("parseQueryStringMap should throw nice errors") {

    locally {
      val ex = intercept[ParsingException] { Map().parseQueryStringMap[QuerySimple] }
      assertEquals(
        ex.errors,
        Seq(
          ParseError("str", "is missing", None),
          ParseError("int", "is missing", None),
          ParseError("uuid", "is missing", None)
        )
      )
    }

    locally {
      val ex = intercept[ParsingException] {
        Map("str" -> Seq(), "int" -> Seq("not_an_int"), "uuid" -> Seq("uuidddd_NOT"))
          .parseQueryStringMap[QuerySimple]
      }
      assertEquals(
        ex.errors,
        Seq(
          ParseError("str", "is missing", None),
          ParseError("int", "invalid Int", Some("not_an_int")),
          ParseError("uuid", "invalid UUID", Some("uuidddd_NOT"))
        )
      )
    }

    locally {
      val ex = intercept[ParsingException] {
        Map("color" -> Seq("Yellow")).parseQueryStringMap[QueryEnum]
      }
      assertEquals(
        ex.errors,
        Seq(ParseError("color", "Enum value not found: 'Yellow'. Possible values: 'Red', 'Blue'", Some("Yellow")))
      )
    }

    // nested
    locally {
      val ex = intercept[ParsingException] {
        Map().parseQueryStringMap[QueryNested]
      }
      assertEquals(ex.errors, Seq(ParseError("search", "is missing", None), ParseError("p", "is missing", None)))
    }

    locally {
      val ex = intercept[ParsingException] {
        Map("p" -> Seq()).parseQueryStringMap[QueryNested]
      }
      assertEquals(
        ex.errors,
        Seq(ParseError("search", "is missing", None), ParseError("p", "should be Object but it is sequence", None))
      )
    }

    locally {
      val ex = intercept[ParsingException] {
        Map("search" -> Seq(""), "p.number" -> Seq("3a")).parseQueryStringMap[QueryNested]
      }
      assertEquals(
        ex.errors,
        Seq(ParseError("p.number", "invalid Int", Some("3a")), ParseError("p.size", "is missing", None))
      )
    }
  }
}
