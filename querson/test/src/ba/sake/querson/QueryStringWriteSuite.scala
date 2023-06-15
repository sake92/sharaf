package ba.sake.querson

import java.util.UUID

class QueryStringWriteSuite extends munit.FunSuite {

  val uuid = UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")

  test("toQueryString should write simple query parameters to string") {
    val res1 = QuerySimple("some text", 42, uuid).toQueryString
    assertEquals(res1, "str=some+text&int=42&uuid=ef42f9e9-79b9-45eb-a938-95ac75aedf87")
  }

  test("toQueryString should write enum query parameters to string") {
    val res1 = QueryEnum(Color.Red).toQueryString
    assertEquals(res1, "color=Red")
  }

  // todo test with brackets a[0]=x&a[1]=y&a[2]=z
  test("toQueryString should write seq query parameters to string") {
    val res1 = QuerySeq(Seq("x", "y", "z")).toQueryString
    assertEquals(res1, "a=x&a=y&a=z")
  }

  // todo test with brackets
  test("toQueryString should write seq query parameters to string") {
    val res1 = QueryNested("what?", Page(5, 42)).toQueryString
    assertEquals(res1, "search=what%3F&p.number=5&p.size=42")
    // assertEquals(res1, "search=what%3F&p[number]=5&p[size]=42")
  }

  test("toQueryString should write seq query parameters to string") {
    val res1 = QueryDefaults(opt = None, seq = Seq.empty).toQueryString
    assertEquals(res1, "q=default")
  }

}
