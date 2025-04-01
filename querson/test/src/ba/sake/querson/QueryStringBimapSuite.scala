package ba.sake.querson

import scala.collection.immutable.SeqMap

class QueryStringBimapSuite extends munit.FunSuite {

  enum Bar:
    case A, B

  given QueryStringRW[Bar] = QueryStringRW[String].bimap(_.toString(), Bar.valueOf)

  case class Foo(bar: Bar)

  test("bimap QueryStringRW read") {
    given QueryStringRW[Foo] = QueryStringRW.derived
    val result = SeqMap(
      "bar" -> Seq("A")
    ).parseQueryStringMap[Foo]
    assertEquals(result.bar, Bar.A)
  }

  test("bimap QueryStringRW write") {
    given QueryStringRW[Foo] = QueryStringRW.derived
    val result = Foo(Bar.B).toQueryStringMap()
    assertEquals(result, SeqMap("bar" -> Seq("B")))
  }

  test("bimap QueryStringRW default") {
    given QueryStringRW[Bar] = QueryStringRW[String].bimap(_.toString(), Bar.valueOf, default = Some(Bar.A))
    given QueryStringRW[Foo] = QueryStringRW.derived
    val result = SeqMap().parseQueryStringMap[Foo]
    assertEquals(result.bar, Bar.A)
  }
}
