package ba.sake.formson

import scala.collection.immutable.SeqMap

class FormDataBimapSuite extends munit.FunSuite {

  enum Bar:
    case A, B

  given FormDataRW[Bar] = FormDataRW[String].bimap(_.toString(), Bar.valueOf)

  case class Foo(bar: Bar)

  test("bimap FormDataRW read") {
    given FormDataRW[Foo] = FormDataRW.derived
    val result = SeqMap(
      "bar" -> Seq(FormValue.Str("A"))
    ).parseFormDataMap[Foo]
    assertEquals(result.bar, Bar.A)
  }

  test("bimap FormDataRW write") {
    given FormDataRW[Foo] = FormDataRW.derived
    val result = Foo(Bar.B).toFormDataMap()
    assertEquals(result, SeqMap("bar" -> Seq(FormValue.Str("B"))))
  }

  test("bimap FormDataRW default") {
    given FormDataRW[Bar] = FormDataRW[String].bimap(_.toString(), Bar.valueOf, default = Some(Bar.A))
    given FormDataRW[Foo] = FormDataRW.derived
    val result = SeqMap().parseFormDataMap[Foo]
    assertEquals(result.bar, Bar.A)
  }
}
