package ba.sake.querson

import java.net.URI
import java.util.UUID
import java.time.*

class QueryStringWriteSuite extends munit.FunSuite {

  val uuid = UUID.fromString("ef42f9e9-79b9-45eb-a938-95ac75aedf87")
  val instant = Instant.parse("2007-12-03T10:15:30.00Z")
  val ldt = LocalDateTime.parse("2007-12-03T10:15:30")
  val period = Period.ofDays(1).plusMonths(4)
  val duration = Duration.ofHours(5).plusSeconds(2)

  val cfgSeqBrackets = DefaultQuersonConfig.withSeqBrackets.withObjBrackets
  val cfgSeqNoBrackets = DefaultQuersonConfig.withSeqNoBrackets.withObjBrackets
  val cfgSeqEmptyBrackets = DefaultQuersonConfig.withSeqEmptyBrackets.withObjBrackets

  val cfgObjBrackets = DefaultQuersonConfig.withSeqNoBrackets.withObjBrackets
  val cfgObjDots = DefaultQuersonConfig.withSeqNoBrackets.withObjDots

  test("toQueryString should write simple query parameters to string") {
    val res1 =
      QuerySimple("some text", Some("optional"), 42, uuid, URI.create("http://example.com"), instant, ldt, duration, period)
        .toQueryString()
    assertEquals(
      res1,
      s"duration=PT5H2S&url=http%3A%2F%2Fexample.com&uuid=$uuid&strOpt%5B0%5D=optional&str=some+text&instant=2007-12-03T10%3A15%3A30Z&int=42&period=P4M1D&ldt=2007-12-03T10%3A15%3A30"
    )
  }

  test("toQueryString should write encode query parameters properly") {
    val res1 = QuerySimpleReservedChars("wh!#at%t he&stu$f?@[]").toQueryString()
    assertEquals(res1, "what%25the%26stu%24f%3F%40%5B%5D=wh%21%23at%25t+he%26stu%24f%3F%40%5B%5D")
  }

  test("toQueryString should write enum query parameters to string") {
    val res1 = QueryEnum(Color.Red).toQueryString()
    assertEquals(res1, "color=Red")
  }

  test("toQueryString should write seq query parameters to string") {
    val queryData = QuerySeq(Seq("x", "y", "z"))
    assertEquals(queryData.toQueryString(cfgSeqNoBrackets), "a=x&a=y&a=z")
    assertEquals(queryData.toQueryString(cfgSeqEmptyBrackets), "a%5B%5D=x&a%5B%5D=y&a%5B%5D=z")
    assertEquals(queryData.toQueryString(cfgSeqBrackets), "a%5B2%5D=z&a%5B0%5D=x&a%5B1%5D=y")
  }

  test("toQueryString should write object query parameters to string") {
    val queryData = QueryNested("what?", Page(5, 42))
    assertEquals(queryData.toQueryString(cfgObjBrackets), "search=what%3F&p%5Bnumber%5D=5&p%5Bsize%5D=42")
    assertEquals(queryData.toQueryString(cfgObjDots), "search=what%3F&p.size=42&p.number=5")
  }

  test("toQueryString should write default query parameters to string") {
    val res1 = QueryDefaults(opt = None, seq = Seq.empty).toQueryString()
    assertEquals(res1, "q=default")
  }

}
