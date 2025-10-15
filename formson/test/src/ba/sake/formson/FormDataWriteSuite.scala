package ba.sake.formson

import java.util.UUID
import scala.collection.SeqMap
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

class FormDataWriteSuite extends munit.FunSuite {

  val uuid = UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")
  val file = Paths.get("test.xml")
  val byteArray = "hello".getBytes(StandardCharsets.UTF_8)

  val cfgSeqBrackets = DefaultFormsonConfig.withSeqBrackets.withObjBrackets
  val cfgSeqNoBrackets = DefaultFormsonConfig.withSeqNoBrackets.withObjBrackets
  val cfgSeqEmptyBrackets = DefaultFormsonConfig.withSeqEmptyBrackets.withObjBrackets

  val cfgObjBrackets = DefaultFormsonConfig.withSeqNoBrackets.withObjBrackets
  val cfgObjDots = DefaultFormsonConfig.withSeqNoBrackets.withObjDots

  test("toFormDataMap should write simple case class") {
    Seq[(FormSimple, FormDataMap)](
      (
        FormSimple("text", None, 42, uuid, file, byteArray, true),
        SeqMap(
          "str" -> Seq("text").map(FormValue.Str.apply),
          "int" -> Seq("42").map(FormValue.Str.apply),
          "uuid" -> Seq(uuid.toString).map(FormValue.Str.apply),
          "file" -> Seq(FormValue.File(file)),
          "bytes" -> Seq(FormValue.ByteArray(byteArray))
        )
      ),
      (
        FormSimple("text", Some("strOptVal"), 42, uuid, file, byteArray, true),
        SeqMap(
          "str" -> Seq("text").map(FormValue.Str.apply),
          "strOpt" -> Seq("strOptVal").map(FormValue.Str.apply),
          "int" -> Seq("42").map(FormValue.Str.apply),
          "uuid" -> Seq(uuid.toString).map(FormValue.Str.apply),
          "file" -> Seq(FormValue.File(file)),
          "bytes" -> Seq(FormValue.ByteArray(byteArray))
        )
      )
    ).foreach { case (data, expected) =>
      val res = data.toFormDataMap(cfgObjDots)
      assertEquals(res, expected)
    }
  }

  test("toFormDataMap should write nested fields") {
    locally {
      val data = FormNested("text", Page(3, 50))
      val expected = SeqMap(
        "search" -> Seq("text").map(FormValue.Str.apply),
        "p.number" -> Seq("3").map(FormValue.Str.apply),
        "p.size" -> Seq("50").map(FormValue.Str.apply)
      )
      val res = data.toFormDataMap(cfgObjDots)
      assertEquals(res, expected)
    }
    locally {
      val data = FormNested("text", Page(3, 50))
      val expected = SeqMap(
        "search" -> Seq("text").map(FormValue.Str.apply),
        "p[number]" -> Seq("3").map(FormValue.Str.apply),
        "p[size]" -> Seq("50").map(FormValue.Str.apply)
      )
      val res = data.toFormDataMap()
      assertEquals(res, expected)
    }
  }

  test("toFormDataMap should write singleton-cases enum") {
    locally {
      val data = FormEnum(Color.Red)
      val expected = SeqMap("color" -> Seq("Red").map(FormValue.Str.apply))
      val res = data.toFormDataMap()
      assertEquals(res, expected)
    }
  }

  test("toFormDataMap should write sealed trait") {
    val sealed1: Sealed1 = Sealed1.Case1("bla", 42)
    assertEquals(
      sealed1.toFormDataMap(),
      SeqMap(
        "str" -> Seq(FormValue.Str("bla")),
        "integer" -> Seq(FormValue.Str("42")),
        "@type" -> Seq(FormValue.Str("Case1"))
      )
    )
    // nested inside
    val nested = NestedSealed1(sealed1)
    assertEquals(
      nested.toFormDataMap(cfgObjDots),
      SeqMap(
        "nest.str" -> Seq(FormValue.Str("bla")),
        "nest.integer" -> Seq(FormValue.Str("42")),
        "nest.@type" -> Seq(FormValue.Str("Case1"))
      )
    )
    // custom discriminator
    val annot = Annot1.B("bla")
    assertEquals(
      annot.toFormDataMap(cfgObjDots),
      SeqMap(
        "tip" -> Seq(FormValue.Str("B")),
        "x" -> Seq(FormValue.Str("bla"))
      )
    )
  }

}
