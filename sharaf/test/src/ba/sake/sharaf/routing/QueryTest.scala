package ba.sake.sharaf
package routing

import scala.util.Try
import java.util.UUID

class QueryTest extends munit.FunSuite {

  test("query params matching") {
    Seq(
      QueryString(),
      QueryString("a" -> Seq()),
      QueryString("a" -> Seq("")),
      QueryString("a" -> Seq("a1")),
      QueryString("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ q[Query1](res) =>
      assertEquals(res.a, qps.params.get("a").toSeq.flatten)
    }

    Seq(
      QueryString(),
      QueryString("a" -> Seq("")),
      QueryString("a" -> Seq("a1")),
      QueryString("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ q[Query2](res) =>
      val expected = qps.params.get("a").flatMap(_.headOption)
      assertEquals(res.a, expected)
    }

    Seq(
      QueryString("a" -> Seq("")),
      QueryString("a" -> Seq("a1")),
      QueryString("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ q[Query3](res) =>
      assertEquals(res.a, qps.params.apply("a").head)
    }

  }

}

case class Query1(a: Seq[String]) derives FromQueryString
case class Query2(a: Option[String]) derives FromQueryString
case class Query3(a: String) derives FromQueryString
