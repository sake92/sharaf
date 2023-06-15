package ba.sake.querson

import java.util.UUID

class QueryStringParamParseSuite extends munit.FunSuite {

  val strRW = QueryStringParamRW[String]
  val intRW = QueryStringParamRW[Int]
  val doubleRW = QueryStringParamRW[Double]
  val uuidRW = QueryStringParamRW[UUID]

  test("QueryStringParamRW should parse query parameter to string") {

    assertEquals(strRW.parse("str", Seq("123", "ignored..")), "123")
    assertEquals(strRW.parse("str", Seq("some text")), "some text")

    assertEquals(intRW.parse("int", Seq("123")), 123)

    assertEquals(doubleRW.parse("d", Seq("123.45")), 123.45)

    assertEquals(
      uuidRW.parse("uuid", Seq("ef42f9e9-79b9-45eb-a938-95ac75aedf87")),
      UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")
    )
  }
}
