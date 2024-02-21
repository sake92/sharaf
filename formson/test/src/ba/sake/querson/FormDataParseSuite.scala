package ba.sake.formson

import java.util.UUID
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.collection.immutable.SeqMap

class FormDataParseSuite extends munit.FunSuite {

  val uuid = UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")
  val file = Paths.get("test.xml")
  val byteArray = "hello".getBytes(StandardCharsets.UTF_8)

  test("parseFormDataMap should parse simple key/values") {
    Seq[(FormDataMap, FormSimple)](
      (
        SeqMap(
          "str" -> Seq("text", "this_is_ignored").map(FormValue.Str.apply),
          "int" -> Seq("42").map(FormValue.Str.apply),
          "uuid" -> Seq(uuid.toString).map(FormValue.Str.apply),
          "file" -> Seq(FormValue.File(file)),
          "bytes" -> Seq(FormValue.ByteArray(byteArray))
        ),
        FormSimple("text", 42, uuid, file, byteArray)
      )
    ).foreach { case (fdMap, expected) =>
      val res = fdMap.parseFormDataMap[FormSimple]
      assertEquals(res, expected)
    }
  }

  test("parseFormDataMap should parse singleton-cases enum") {
    Seq[(FormDataMap, FormEnum)](
      (SeqMap("color" -> Seq("Red").map(FormValue.Str.apply)), FormEnum(Color.Red))
    ).foreach { case (fdMap, expected) =>
      val res = fdMap.parseFormDataMap[FormEnum]
      assertEquals(res, expected)
    }
  }

  test("parseFormDataMap should parse sequence") {
    Seq[(FormDataMap, FormSeq)](
      (SeqMap(), FormSeq(Seq())),
      (SeqMap("a" -> Seq()), FormSeq(Seq())),
      (SeqMap("a" -> Seq("").map(FormValue.Str.apply)), FormSeq(Seq(""))),
      (SeqMap("a" -> Seq("a1").map(FormValue.Str.apply)), FormSeq(Seq("a1"))),
      (SeqMap("a" -> Seq("a1", "a2").map(FormValue.Str.apply)), FormSeq(Seq("a1", "a2"))),
      (SeqMap("a[]" -> Seq("a1", "a2").map(FormValue.Str.apply)), FormSeq(Seq("a1", "a2"))),
      (
        SeqMap(
          "a[3]" -> Seq("a3").map(FormValue.Str.apply),
          "a" -> Seq("a0", "a00").map(FormValue.Str.apply),
          "a[]" -> Seq("a0_1", "a0_11").map(FormValue.Str.apply),
          "a[1]" -> Seq("a1").map(FormValue.Str.apply)
        ),
        FormSeq(Seq("a0", "a00", "a0_1", "a0_11", "a1", "a3")) // sorted nicely
      )
    ).foreach { case (fdMap, expected) =>
      val res = fdMap.parseFormDataMap[FormSeq]
      assertEquals(res, expected)
    }
  }

  // TODO ???????
  test("parseFormDataMap should parse sequence of sequences") {
    Seq[(FormDataMap, FormSeqSeq)](
      (SeqMap(), FormSeqSeq(Seq())),
      (SeqMap("a" -> Seq()), FormSeqSeq(Seq())),
      (SeqMap("a[][]" -> Seq("").map(FormValue.Str.apply)), FormSeqSeq(Seq(Seq(""))))
      //  (SeqMap("a" -> Seq("a1")), FormSeqSeq(Seq("a1"))),
      //  (SeqMap("a" -> Seq("a1", "a2")), FormSeqSeq(Seq("a1", "a2"))),
      //  (SeqMap("a[]" -> Seq("a1", "a2")), FormSeqSeq(Seq("a1", "a2"))),
      /*(
        SeqMap(
          "a[3]" -> Seq("a3"),
          "a" -> Seq("a0", "a00"),
          "a[]" -> Seq("a0_1", "a0_11"),
          "a[1]" -> Seq("a1")
        ),
        FormSeqSeq(Seq("a0", "a00", "a0_1", "a0_11", "a1", "a3")) // sorted nicely
      )*/
    ).foreach { case (fdMap, expected) =>
      val res = fdMap.parseFormDataMap[FormSeqSeq]
      assertEquals(res, expected)
    }
  }

  test("parseFormDataMap should parse nested fields") {
    Seq[(FormDataMap, FormNested)](
      (
        SeqMap(
          "search" -> Seq("text", "this_is_ignored").map(FormValue.Str.apply),
          "p.number" -> Seq("3").map(FormValue.Str.apply),
          "p.size" -> Seq("50").map(FormValue.Str.apply)
        ),
        FormNested("text", Page(3, 50))
      ),
      (
        SeqMap(
          "search" -> Seq("text", "this_is_ignored").map(FormValue.Str.apply),
          "p[number]" -> Seq("3").map(FormValue.Str.apply),
          "p[size]" -> Seq("50").map(FormValue.Str.apply)
        ),
        FormNested("text", Page(3, 50))
      )
    ).foreach { case (fdMap, expected) =>
      val res = fdMap.parseFormDataMap[FormNested]
      assertEquals(res, expected)
    }
  }

  test("parseFormDataMap should parse falling back to defaults") {
    Seq[(FormDataMap, FormDefaults)](
      (
        SeqMap(),
        FormDefaults("default", None, Seq())
      ),
      (
        SeqMap(
          "q" -> Seq("q1").map(FormValue.Str.apply),
          "opt" -> Seq("optValue").map(FormValue.Str.apply),
          "seq" -> Seq("seq1", "seq2").map(FormValue.Str.apply)
        ),
        FormDefaults("q1", Some("optValue"), Seq("seq1", "seq2"))
      )
    ).foreach { case (fdMap, expected) =>
      val res = fdMap.parseFormDataMap[FormDefaults]
      assertEquals(res, expected)
    }
  }

  test("parseFormDataMap should throw nice errors") {

    locally {
      val ex = intercept[ParsingException] { SeqMap().parseFormDataMap[FormSimple] }
      assertEquals(
        ex.errors,
        Seq(
          ParseError("str", "is missing", None),
          ParseError("int", "is missing", None),
          ParseError("uuid", "is missing", None),
          ParseError("file", "is missing", None),
          ParseError("bytes", "is missing", None)
        )
      )
    }

    locally {
      val ex = intercept[ParsingException] {
        SeqMap(
          "str" -> Seq(),
          "int" -> Seq("not_an_int").map(FormValue.Str.apply),
          "uuid" -> Seq("uuidddd_NOT").map(FormValue.Str.apply),
          "file" -> Seq(),
          "bytes" -> Seq()
        )
          .parseFormDataMap[FormSimple]
      }
      assertEquals(
        ex.errors,
        Seq(
          ParseError("str", "is missing", None),
          ParseError("int", "invalid Int", Some("not_an_int")),
          ParseError("uuid", "invalid UUID", Some("uuidddd_NOT")),
          ParseError("file", "is missing", None),
          ParseError("bytes", "is missing", None)
        )
      )
    }

    locally {
      val ex = intercept[ParsingException] {
        SeqMap("color" -> Seq("Yellow").map(FormValue.Str.apply)).parseFormDataMap[FormEnum]
      }
      assertEquals(
        ex.errors,
        Seq(ParseError("color", "Enum value not found: 'Yellow'. Possible values: 'Red', 'Blue'", Some("Yellow")))
      )
    }

    // nested
    locally {
      val ex = intercept[ParsingException] {
        SeqMap().parseFormDataMap[FormNested]
      }
      assertEquals(ex.errors, Seq(ParseError("search", "is missing", None), ParseError("p", "is missing", None)))
    }

    locally {
      val ex = intercept[ParsingException] {
        SeqMap("p" -> Seq()).parseFormDataMap[FormNested]
      }
      assertEquals(
        ex.errors,
        Seq(ParseError("search", "is missing", None), ParseError("p", "should be Object but it is sequence", None))
      )
    }

    locally {
      val ex = intercept[ParsingException] {
        SeqMap("search" -> Seq("").map(FormValue.Str.apply), "p.number" -> Seq("3a").map(FormValue.Str.apply))
          .parseFormDataMap[FormNested]
      }
      assertEquals(
        ex.errors,
        Seq(ParseError("p.number", "invalid Int", Some("3a")), ParseError("p.size", "is missing", None))
      )
    }
  }
}
