package ba.sake.sharaf
package routing

import scala.util.Try
import java.util.UUID

class RoutingTest extends munit.FunSuite {

  test("path matching") {
    val uuidValue = UUID.randomUUID
    val paths = Seq(
      Path("users", "1"),
      Path("users", uuidValue.toString),
      Path("users", "email"),
      Path("users", "abc"),
      Path("users", "what", "the", "stuff")
    )

    paths.foreach {
      case Path("users", int(id)) =>
        assertEquals(id, 1)
      case Path("users", uuid(id)) =>
        assertEquals(id, uuidValue)
      case Path("users", param[Sort](sort)) =>
        assertEquals(sort, Sort.email)
      case Path("users", id) =>
        assertEquals(id, "abc")
      case Path("users", parts*) =>
        assertEquals(parts, Seq("what", "the", "stuff"))
    }
  }

  test("query params matching") {
    Seq(
      QueryString("a" -> Seq()),
      QueryString("a" -> Seq("")),
      QueryString("a" -> Seq("a1")),
      QueryString("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ ?("a" -> qSeq(as)) =>
      assertEquals(as, qps.params.toMap.apply("a"))
    }

    Seq(
      QueryString("a" -> Seq("")),
      QueryString("a" -> Seq("a1")),
      QueryString("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ ?("a" -> q(a)) =>
      assertEquals(a, qps.params.toMap.apply("a").head)
    }

    Seq(
      QueryString("a" -> Seq("")),
      QueryString("a" -> Seq("a1")),
      QueryString("a" -> Seq("a1", "a2"))
    ).foreach { case qps @ ?("a" -> qOpt(aOpt)) =>
      val res = qps.params.toMap.get("a").flatMap(_.headOption)
      assertEquals(aOpt, res)
    }
  }

}

enum Sort extends java.lang.Enum[Sort]:
  case email, name

given FromUrlParam[Sort] = new {
  override def extract(str: String): Option[Sort] =
    Try(Sort.valueOf(str)).toOption
}
