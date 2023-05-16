package ba.sake.formson

import scala.collection.immutable.SortedMap

class FormsonTest extends munit.FunSuite {

  test("parse simple key-value form map") {
    import FormDataInternal.*
    val rawMap: FlatFormValues = Map(
      "a" -> Seq(FormValue.Str("a1"), FormValue.Str("a2"))
    )

    val parser = new FormsonParser(rawMap)
    val result = parser.parse()

    val expected: FormDataInternal.Obj = Obj(Map("a" -> Simple(FormValue.Str("a1"))))
    assertEquals(result, expected)
  }
  test("parse nested key-values form map") {
    import FormDataInternal.*
    val rawMap: FlatFormValues = Map(
      "b.b1" -> Seq(FormValue.Str("bbb")),
      "b.b2.b21" -> Seq(FormValue.Str("bb2")),
      "b.bs[]" -> Seq(FormValue.Str("123"))
    )

    val parser = new FormsonParser(rawMap)
    val result = parser.parse()

    val expected: FormDataInternal.Obj = Obj(
      Map(
        "b" -> Obj(
          Map(
            "b1" -> Simple(FormValue.Str("bbb")),
            "b2" -> Obj(Map("b21" -> Simple(FormValue.Str("bb2")))),
            "bs" -> Sequence(SortedMap(0 -> Seq(Simple(FormValue.Str("123")))))
          )
        )
      )
    )
    assertEquals(result, expected)
  }
  test("parse sequence key-values form map") {
    import FormDataInternal.*
    val rawMap: FlatFormValues = Map(
      "s[1]" -> Seq(FormValue.Str("s1")),
      "s[]" -> Seq(FormValue.Str("s_empty_brackets"), FormValue.Str("s_empty_brackets2")),
      "s[0]" -> Seq(FormValue.Str("s0"))
    )

    val parser = new FormsonParser(rawMap)
    val result = parser.parse()

    val expected: FormDataInternal.Obj = Obj(
      Map(
        "s" -> Sequence(
          SortedMap(
            0 -> List(
              Simple(FormValue.Str("s_empty_brackets")),
              Simple(FormValue.Str("s_empty_brackets2")),
              Simple(FormValue.Str("s0"))
            ),
            1 -> List(Simple(FormValue.Str("s1")))
          )
        )
      )
    )
    assertEquals(result, expected)
  }
}

// TODO
case class MyForm(
    a: String,
    b: B,
    s: List[String]
) derives FromFormData

case class B(b1: String, bs: List[Int])
