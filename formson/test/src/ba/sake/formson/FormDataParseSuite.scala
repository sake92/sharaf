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
          "strOpt" -> Seq("").map(FormValue.Str.apply), // empty string is considered None !
          "int" -> Seq("42").map(FormValue.Str.apply),
          "uuid" -> Seq(uuid.toString).map(FormValue.Str.apply),
          "file" -> Seq(FormValue.File(file)),
          "bytes" -> Seq(FormValue.ByteArray(byteArray)),
          "bool" -> Seq(FormValue.Str("true"))
        ),
        FormSimple("text", None, 42, uuid, file, byteArray, true)
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

  test("parseFormDataMap should parse nested fields of sequences") {
    locally {
      val data = FormNestedSeq("text", Seq(Page(3, 50), Page(4, 60)))
      val formDataMap = SeqMap(
        "search" -> Seq("text").map(FormValue.Str.apply),
        "pages[0][number]" -> Seq("3").map(FormValue.Str.apply),
        "pages[0][size]" -> Seq("50").map(FormValue.Str.apply),
        "pages[1][number]" -> Seq("4").map(FormValue.Str.apply),
        "pages[1][size]" -> Seq("60").map(FormValue.Str.apply)
      )
      assertEquals(formDataMap.parseFormDataMap[FormNestedSeq], data)
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

  test("parseFormDataMap should parse sealed trait") {
    assertEquals(
      SeqMap(
        "str" -> Seq(FormValue.Str("bla")),
        "integer" -> Seq(FormValue.Str("42")),
        "@type" -> Seq(FormValue.Str("Case1"))
      ).parseFormDataMap[Sealed1],
      Sealed1.Case1("bla", 42)
    )
    // nested inside
    assertEquals(
      SeqMap(
        "nest.str" -> Seq(FormValue.Str("bla")),
        "nest.integer" -> Seq(FormValue.Str("42")),
        "nest.@type" -> Seq(FormValue.Str("Case1"))
      ).parseFormDataMap[NestedSealed1],
      NestedSealed1(
        Sealed1.Case1("bla", 42)
      )
    )
    // custom discriminator
    assertEquals(
      SeqMap(
        "tip" -> Seq(FormValue.Str("B")),
        "x" -> Seq(FormValue.Str("bla"))
      ).parseFormDataMap[Annot1],
      Annot1.B("bla")
    )
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
          ParseError("bytes", "is missing", None),
          ParseError("bool", "is missing", None)
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
          "bytes" -> Seq(),
          "bool" -> Seq()
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
          ParseError("bytes", "is missing", None),
          ParseError("bool", "is missing", None)
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

  test("parseFormDataMap named tuple") {
    val res = SeqMap("q" -> Seq("searchme").map(FormValue.Str.apply), "page" -> Seq("42").map(FormValue.Str.apply))
      .parseFormDataMap[(q: String, page: Int)]
    assertEquals(res, (q = "searchme", page = 42))

    val present = SeqMap("opt" -> Seq("value").map(FormValue.Str.apply)).parseFormDataMap[(opt: Option[String])]
    assertEquals(present, (opt = Some("value")))

    val empty = SeqMap("opt" -> Seq("").map(FormValue.Str.apply)).parseFormDataMap[(opt: Option[String])]
    assertEquals(empty, (opt = None))

    val absent = SeqMap.empty[String, Seq[FormValue]].parseFormDataMap[(opt: Option[String])]
    assertEquals(absent, (opt = None))

    val invalid = intercept[ParsingException] {
      SeqMap("opt" -> Seq("not_an_int").map(FormValue.Str.apply)).parseFormDataMap[(opt: Option[Int])]
    }
    assertEquals(invalid.errors, Seq(ParseError(".opt[0]", "invalid Int", Some("not_an_int"))))
  }

  test("parse and write Option wrapping a named tuple") {
    type Named = (name: String, age: Int)
    val value: (person: Option[Named]) = (person = Some((name = "Ada", age = 37)))
    val serialized = value.toFormDataMap()
    assertEquals(
      serialized,
      SeqMap(
        "person[0][name]" -> Seq(FormValue.Str("Ada")),
        "person[0][age]" -> Seq(FormValue.Str("37"))
      )
    )

    assertEquals(serialized.parseFormDataMap[(person: Option[Named])], value)

    val noBrackets = value.toFormDataMap(DefaultFormsonConfig.withSeqNoBrackets.withObjDots)
    assertEquals(
      noBrackets,
      SeqMap(
        "person.name" -> Seq(FormValue.Str("Ada")),
        "person.age" -> Seq(FormValue.Str("37"))
      )
    )
    assertEquals(noBrackets.parseFormDataMap[(person: Option[Named])], value)

    val noBracketsInvalid = intercept[ParsingException] {
      SeqMap(
        "person.name" -> Seq(FormValue.Str("Ada")),
        "person.age" -> Seq(FormValue.Str("not_an_int"))
      ).parseFormDataMap[(person: Option[Named])]
    }
    assertEquals(noBracketsInvalid.errors, Seq(ParseError(".person.age", "invalid Int", Some("not_an_int"))))

    val absent = SeqMap.empty[String, Seq[FormValue]].parseFormDataMap[(person: Option[Named])]
    assertEquals(absent, (person = None))

    val empty = SeqMap("person" -> Seq(FormValue.Str(""))).parseFormDataMap[(person: Option[Named])]
    assertEquals(empty, (person = None))

    val invalid = intercept[ParsingException] {
      SeqMap(
        "person[0][name]" -> Seq(FormValue.Str("Ada")),
        "person[0][age]" -> Seq(FormValue.Str("not_an_int"))
      ).parseFormDataMap[(person: Option[Named])]
    }
    assertEquals(invalid.errors, Seq(ParseError(".person[0].age", "invalid Int", Some("not_an_int"))))

    val nested: (person: Option[Option[Named]]) = (person = Some(Some((name = "Ada", age = 37))))
    assertEquals(
      nested.toFormDataMap().parseFormDataMap[(person: Option[Option[Named]])],
      nested
    )
  }

  test("parse union type") {
    locally {
      val res = SeqMap("id" -> Seq("myid1").map(FormValue.Str.apply)).parseFormDataMap[(id: String | Int)]
      assertEquals(res, (id = "myid1"))
    }
    locally {
      val res = SeqMap("stuff" -> Seq("true").map(FormValue.Str.apply)).parseFormDataMap[(stuff: Int | Boolean)]
      assertEquals(res, (stuff = true))
    }
    locally {
      val res1 = SeqMap("color" -> Seq("Red").map(FormValue.Str.apply)).parseFormDataMap[FormEnum | FormSeq]
      assertEquals(res1, FormEnum(Color.Red))
      val res2 = SeqMap("a" -> Seq("Red").map(FormValue.Str.apply)).parseFormDataMap[FormEnum | FormSeq]
      assertEquals(res2, FormSeq(Seq("Red")))
    }
    locally { // combining named tuples with a union
      val res1 = SeqMap("firstname" -> Seq("Mujo").map(FormValue.Str.apply)).parseFormDataMap[(firstname: String) | (lastname: String)]
      assertEquals(res1, (firstname = "Mujo"))
      val res2 = SeqMap("lastname" -> Seq("Hrnjica").map(FormValue.Str.apply)).parseFormDataMap[(firstname: String) | (lastname: String)]
      assertEquals(res2, (lastname = "Hrnjica"))
    }
    locally {
      val res = SeqMap("right" -> Seq("value").map(FormValue.Str.apply))
        .parseFormDataMap[(left: Option[String]) | (right: String)]
      assertEquals(res, (right = "value"))
      val absent = SeqMap.empty[String, Seq[FormValue]]
        .parseFormDataMap[(left: Option[String]) | (right: String)]
      assertEquals(absent, (left = None))
      val ambiguous = SeqMap(
        "left" -> Seq(FormValue.Str("left")),
        "right" -> Seq(FormValue.Str("right"))
      ).parseFormDataMap[(left: Option[String]) | (right: String)]
      assertEquals(ambiguous, (left = Some("left")))
    }
    locally {
      type First = (first: Option[String], marker: Option[Int])
      type Second = (second: Option[String])
      type Third = (third: String)

      val suppliedFirst = SeqMap(
        "first" -> Seq(FormValue.Str("one")),
        "marker" -> Seq(FormValue.Str("2"))
      ).parseFormDataMap[First | Second | Third]
      assertEquals(suppliedFirst, (first = Some("one"), marker = Some(2)))

      val suppliedThird = SeqMap("third" -> Seq(FormValue.Str("three")))
        .parseFormDataMap[First | Second | Third]
      assertEquals(suppliedThird, (third = "three"))
    }
    locally {
      val res = SeqMap("status" -> Seq("2").map(FormValue.Str.apply)).parseFormDataMap[(status: 1 | 2 | 3)]
      assertEquals(res, (status = 2))
    }
  }
}
