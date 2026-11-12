package ba.sake.sharaf

import ba.sake.querson.*

case class FlatQuery(a: Long = 0L, b: Long = 0L) derives QueryStringRW
case class Page(number: Int, size: Int) derives QueryStringRW
case class NestedQuery(search: String, p: Page) derives QueryStringRW
case class RepeatedQuery(a: Seq[String]) derives QueryStringRW

object ParserBenchmark {
  def main(args: Array[String]): Unit = {
    val flat = Array(Map("a" -> Seq("13"), "b" -> Seq("42")), Map("a" -> Seq("24"), "b" -> Seq("13")))
    val nested = Array(
      Map("search" -> Seq("text"), "p.number" -> Seq("3"), "p.size" -> Seq("50")),
      Map("search" -> Seq("other"), "p[number]" -> Seq("4"), "p[size]" -> Seq("20"))
    )
    val repeated = Array(Map("a[1]" -> Seq("one"), "a[0]" -> Seq("zero")), Map("a[]" -> Seq("abc", "def")))
    val ast = flat.map(parseQSMap)
    assert(flat(0).parseQueryStringMap[FlatQuery] == FlatQuery(13, 42))
    assert(nested(0).parseQueryStringMap[NestedQuery] == NestedQuery("text", Page(3, 50)))
    assert(repeated(0).parseQueryStringMap[RepeatedQuery] == RepeatedQuery(Seq("zero", "one")))
    assert(UrlEncoded.query("a=1&a=2&b=3")("a") == Seq("1", "2"))
    assert(UrlEncoded.query("a=hello%20world&b=x%2By")("b") == Seq("x+y"))
    val parsers: Array[(String, Int => Long)] = Array(
      "url_plain" -> ((i: Int) => {
        val m = UrlEncoded.query(if ((i & 1) == 0) "a=13&b=42" else "a=24&b=13"); (m.size + m("a").head.length).toLong
      }),
      "url_escaped" -> ((i: Int) => {
        val m = UrlEncoded.query(if ((i & 1) == 0) "a=hello%20world&b=x%2By" else "a=caf%C3%A9&b=two+words");
        (m.size + m("a").head.length).toLong
      }),
      "url_repeated" -> ((i: Int) => {
        val m = UrlEncoded.query(if ((i & 1) == 0) "a=1&a=2&b=3" else "b=4&a=7&a=9"); (m.size + m("a").size).toLong
      }),
      "url_empty" -> ((i: Int) => { val m = UrlEncoded.query(if ((i & 1) == 0) "" else "a=&b="); m.size.toLong }),
      "querson_flat_ast" -> ((i: Int) => parseQSMap(flat(i & 1)).asInstanceOf[QueryStringData.Obj].values.size.toLong),
      "querson_flat_reader" -> ((i: Int) => { val q = QueryStringRW[FlatQuery].parse("", ast(i & 1)); q.a + q.b }),
      "querson_flat_full" -> ((i: Int) => { val q = flat(i & 1).parseQueryStringMap[FlatQuery]; q.a + q.b }),
      "querson_nested_full" -> ((i: Int) => {
        val q = nested(i & 1).parseQueryStringMap[NestedQuery]; (q.p.number + q.p.size + q.search.length).toLong
      }),
      "querson_indexed_full" -> ((i: Int) => {
        val q = repeated(i & 1).parseQueryStringMap[RepeatedQuery]; (q.a.size + q.a.head.length).toLong
      })
    )
    var sink = 0L
    for ((name, operation) <- parsers) {
      for (round <- 0 until 8) {
        val iterations = 50000
        val start = System.nanoTime()
        var i = 0
        var checksum = 0L
        while (i < iterations) { checksum += operation(i); i += 1 }
        val elapsed = System.nanoTime() - start
        sink += checksum
        if (round >= 3) println(s"$name round=$round ns_per_op=${elapsed / iterations} checksum=$checksum")
      }
    }
    println(s"sink=$sink")
  }
}
