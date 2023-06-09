package ba.sake.sharaf
package routing

import ba.sake.querson.*

class QueryTest extends munit.FunSuite {

  test("query params matching") {
    Seq[RawQueryString](
      Map(),
      Map("a" -> Seq())
      //Map("a" -> Seq("")),
      //Map("a" -> Seq("a1")),
      //Map("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ q[Query1](res) =>
      assertEquals(res.a, qps.get("a").toSeq.flatten)
    }

    Seq(
      Map(),
      Map("a" -> Seq("")),
      Map("a" -> Seq("a1")),
      Map("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ q[Query2](res) =>
      val expected = qps.get("a").flatMap(_.headOption)
      assertEquals(res.a, expected)
    }

    Seq(
      Map("a" -> Seq("")),
      Map("a" -> Seq("a1")),
      Map("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ q[Query3](res) =>
      assertEquals(res.a, qps.apply("a").head)
    }

  }

}

case class Query1(a: Seq[String]) derives QueryStringRW
case class Query2(a: Option[String]) derives QueryStringRW
case class Query3(a: String) derives QueryStringRW
