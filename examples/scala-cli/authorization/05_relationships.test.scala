package authorization

import authorization.relationships.{RelationshipsExample, Course}

class RelationshipsSuite extends HttpExampleSuite:
  test("teacher plus student can teach, while a student-only member cannot") {
    withServer(RelationshipsExample.server) { base =>
      val changed = decoded[Course](request(base, "/courses/a", "PUT", Some("alice"), """{"title":"Revised A"}"""))
      assertEquals(changed, Course("a", "Revised A", false))
      assertEquals(decoded[Course](request(base, "/courses/a", user = Some("bob"))), changed)
      assertEquals(request(base, "/courses/a", "PUT", Some("bob"), """{"title":"Student edit"}""").code.code, 403)
      assertEquals(decoded[Course](request(base, "/courses/a", user = Some("alice"))), changed)
    }
  }

  test("teaching membership is scoped to the actual course and user") {
    withServer(RelationshipsExample.server) { base =>
      assertEquals(request(base, "/courses/a", "PUT", Some("alice"), """{"title":"A changed"}""").code.code, 200)
      val forged = """{"title":"Unauthorized B","courseId":"a","roles":["Teacher"],"userId":"bob"}"""
      assertEquals(request(base, "/courses/b", "PUT", Some("alice"), forged).code.code, 403)
      assertEquals(decoded[Course](request(base, "/courses/b", user = Some("alice"))), Course("b", "Course B", false))
      assertEquals(request(base, "/courses/b", "PUT", Some("bob"), """{"title":"Teacher B"}""").code.code, 200)
      assertEquals(request(base, "/courses/b").code.code, 401)
      assertEquals(request(base, "/courses/b", user = Some("outsider")).code.code, 404)
    }
  }

  test("teacher cannot edit an archived course, and nonmembers see no resource") {
    withServer(RelationshipsExample.server) { base =>
      assertEquals(request(base, "/courses/archived", "PUT", Some("alice"), """{"title":"Reopened"}""").code.code, 403)
      val archived = decoded[Course](request(base, "/courses/archived", user = Some("alice")))
      assertEquals(archived, Course("archived", "Archived", true))
      assertEquals(request(base, "/courses/a", "PUT", Some("outsider"), """{"title":"Intrusion"}""").code.code, 404)
      assertEquals(request(base, "/courses/missing", user = Some("alice")).code.code, 404)
      assertEquals(decoded[Course](request(base, "/courses/a", user = Some("alice"))), Course("a", "Course A", false))
    }
  }
