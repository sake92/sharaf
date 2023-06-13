package ba.sake.querson

import java.util.UUID

class QueryStringRWTest extends munit.FunSuite {

  val uuid = UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")

  test("parseQueryString should parse simple key/values") {
    Seq[(RawQueryString, QuerySimple)](
      (
        Map(
          "str" -> Seq("text", "this_is_ignored"),
          "int" -> Seq("42"),
          "uuid" -> Seq(uuid.toString)
        ),
        QuerySimple("text", 42, uuid)
      )
    ).foreach { case (rawQS, expected) =>
      val res = rawQS.parseQueryString[QuerySimple]
      assertEquals(res, expected)
    }
  }

  test("parseQueryString should parse singleton-cases enum") {
    Seq[(RawQueryString, QueryEnum)](
      (Map("color" -> Seq("Red")), QueryEnum(Color.Red))
    ).foreach { case (rawQS, expected) =>
      val res = rawQS.parseQueryString[QueryEnum]
      assertEquals(res, expected)
    }
  }

  test("parseQueryString should parse sequence") {
    Seq[(RawQueryString, QuerySeq)](
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
    ).foreach { case (rawQS, expected) =>
      val res = rawQS.parseQueryString[QuerySeq]
      assertEquals(res, expected)
    }
  }

  test("parseQueryString should parse nested fields") {
    Seq[(RawQueryString, QueryNested)](
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
    ).foreach { case (rawQS, expected) =>
      val res = rawQS.parseQueryString[QueryNested]
      assertEquals(res, expected)
    }
  }

  test("parseQueryString should parse falling back to defaults") {
    Seq[(RawQueryString, QueryDefaults)](
      (
        Map(),
        QueryDefaults("default", None, Seq())
      ),
      (
        Map("q" -> Seq("q1"), "opt" -> Seq("optValue"), "seq" -> Seq("seq1", "seq2")),
        QueryDefaults("q1", Some("optValue"), Seq("seq1", "seq2"))
      )
    ).foreach { case (rawQS, expected) =>
      val res = rawQS.parseQueryString[QueryDefaults]
      assertEquals(res, expected)
    }
  }
}

case class QuerySimple(str: String, int: Int, uuid: UUID) derives QueryStringRW

enum Color derives QueryStringParamRW:
  case Red
  case Blue
case class QueryEnum(color: Color) derives QueryStringRW

case class QuerySeq(a: Seq[String]) derives QueryStringRW

case class QueryNested(search: String, p: Page) derives QueryStringRW
case class Page(number: Int, size: Int) derives QueryStringRW

// Option and Seq have global defaults (in typeclass instance)
case class QueryDefaults(q: String = "default", opt: Option[String], seq: Seq[String]) derives QueryStringRW
