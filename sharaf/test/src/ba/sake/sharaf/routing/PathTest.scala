package ba.sake.sharaf
package routing

import scala.util.Try
import java.util.UUID

class PathTest extends munit.FunSuite {

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
      case Path("users", param[Int](id)) =>
        assertEquals(id, 1)
      case Path("users", param[UUID](id)) =>
        assertEquals(id, uuidValue)
      case Path("users", param[Sort](sort)) =>
        assertEquals(sort, Sort.email)
      case Path("users", id) =>
        assertEquals(id, "abc")
      case Path("users", parts*) =>
        assertEquals(parts, Seq("what", "the", "stuff"))
    }
  }

}

enum Sort extends java.lang.Enum[Sort]:
  case email, name

given FromPathParam[Sort] = new {
  override def extract(str: String): Option[Sort] =
    Try(Sort.valueOf(str)).toOption
}
