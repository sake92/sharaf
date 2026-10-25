package authorization

import authorization.abac.{AbacExample, Document as AttributeDocument}

class AbacSuite extends HttpExampleSuite:
  test("ABAC allows active subscribed owner of an unarchived document") {
    withServer(AbacExample.server) { base =>
      val rename = request(base, "/documents/draft", "PUT", Some("alice"), """{"title":"Published draft"}""")
      val changed = decoded[AttributeDocument](rename)
      assertEquals(changed, AttributeDocument("draft", "alice", "Published draft", false))
      assertEquals(decoded[AttributeDocument](request(base, "/documents/draft", user = Some("alice"))), changed)
    }
  }

  for (actor, documentId, owner, title, archived) <- List(
    ("bob", "draft", "alice", "Draft", false),
    ("trial", "trial", "trial", "Trial", false),
    ("inactive", "inactive", "inactive", "Inactive", false),
    ("alice", "archived", "alice", "Archived", true)
  ) do
    test(s"ABAC denies $actor editing $documentId and preserves state") {
      withServer(AbacExample.server) { base =>
        val original = AttributeDocument(documentId, owner, title, archived)
        val forged = s"""{"title":"Forbidden change","ownerId":"$actor",
          "archived":false,"subscribed":true,"active":true}"""
        assertEquals(request(base, s"/documents/$documentId", "PUT", Some(actor), forged).code.code, 403)
        assertEquals(decoded[AttributeDocument](request(base, s"/documents/$documentId", user = Some(owner))), original)
      }
    }

  test("ABAC unknown resource and anonymous mutations are denied") {
    withServer(AbacExample.server) { base =>
      val body = """{"title":"No access"}"""
      assertEquals(request(base, "/documents/draft", "PUT", body = body).code.code, 401)
      assertEquals(request(base, "/documents/missing", "PUT", Some("alice"), body).code.code, 404)
      assertEquals(decoded[AttributeDocument](request(base, "/documents/draft", user = Some("alice"))).title, "Draft")
    }
  }
