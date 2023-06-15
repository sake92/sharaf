package ba.sake.querson

import java.util.UUID

class QueryStringParamWriteSuite extends munit.FunSuite {

  val strRW = QueryStringParamRW[String]
  val intRW = QueryStringParamRW[Int]
  val doubleRW = QueryStringParamRW[Double]
  val uuidRW = QueryStringParamRW[UUID]

  val seqStrRW = QueryStringParamRW[Seq[String]]
  val seqDoubleRW = QueryStringParamRW[Seq[Double]]

  test("QueryStringParamRW should write encoded query parameter to string") {

    assertEquals(strRW.write("str", "123"), "str=123")
    assertEquals(strRW.write("str", "some text"), "str=some+text")

    assertEquals(intRW.write("int", 123), "int=123")

    assertEquals(doubleRW.write("d", 123.45), "d=123.45")

    assertEquals(
      uuidRW.write("uuid", UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")),
      "uuid=ef42f9e9-79b9-45eb-a938-95ac75aedf87"
    )

    assertEquals(seqStrRW.write("seq", Seq()), "")
    assertEquals(seqStrRW.write("seq", Seq("a")), "seq=a")
    assertEquals(seqStrRW.write("seq", Seq("a", "b")), "seq=a&seq=b")

    assertEquals(seqDoubleRW.write("seq", Seq()), "")
    assertEquals(seqDoubleRW.write("seq", Seq(0.5)), "seq=0.5")

  }
}
