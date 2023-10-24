package ba.sake.sharaf
package routing

import java.util.UUID

class PathTest extends munit.FunSuite {

  test("path matching") {
    val uuidValue = UUID.randomUUID

    Path("users", "1") match
      case Path("users", param[Int](id)) =>
        assertEquals(id, 1)
      case _ =>
        fail("Did not match route")

    Path("users", uuidValue.toString) match
      case Path("users", param[UUID](id)) =>
        assertEquals(id, uuidValue)
      case _ =>
        fail("Did not match route")

    Path("users", "email") match
      case Path("users", param[Sort](sort)) =>
        assertEquals(sort, Sort.email)
      case _ =>
        fail("Did not match route")

    Path("users", "abc") match
      case Path("users", id) =>
        assertEquals(id, "abc")
      case _ =>
        fail("Did not match route")

    Path("users", "what", "the", "stuff") match
      case Path("users", parts*) =>
        assertEquals(parts, Seq("what", "the", "stuff"))
      case _ =>
        fail("Did not match route")

    val userIdRegex = "user_id_(\\d+)".r
    Path("users", "user_id_456") match
      case Path("users", userIdRegex(userId)) =>
        assertEquals(userId, "456")
      case _ =>
        fail("Did not match route")

    // nesting, noice
    Path("users", "user_id_456") match
      case Path("users", userIdRegex(param[Int](userId))) =>
        assertEquals(userId, 456)
      case _ =>
        fail("Did not match route")

  }

}

enum Sort derives FromPathParam:
  case email, name
