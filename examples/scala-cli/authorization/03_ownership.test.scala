package authorization

import authorization.ownership.{OwnershipExample, Document as OwnedDocument}

class OwnershipSuite extends HttpExampleSuite:
  test("ownership boundaries preserve state after denied reads, writes, and deletes") {
    withServer(OwnershipExample.server) { base =>
      val original = OwnedDocument("private", "alice", "Original")
      assertEquals(decoded[OwnedDocument](request(base, "/documents/private", user = Some("alice"))), original)
      assertEquals(request(base, "/documents/private", user = Some("bob")).code.code, 404)
      assertEquals(request(base, "/documents/missing", user = Some("alice")).code.code, 404)
      assertEquals(request(base, "/documents/private", "PUT", Some("bob"), """{"title":"Stolen"}""").code.code, 404)
      assertEquals(request(base, "/documents/private", "DELETE", Some("bob")).code.code, 404)
      assertEquals(decoded[OwnedDocument](request(base, "/documents/private", user = Some("alice"))), original)
      val rename = request(base, "/documents/private", "PUT", Some("alice"), """{"title":"Renamed"}""")
      val updated = decoded[OwnedDocument](rename)
      assertEquals(updated, original.copy(title = "Renamed"))
      assertEquals(request(base, "/documents/private", "DELETE", Some("alice")).code.code, 204)
      assertEquals(request(base, "/documents/private", user = Some("alice")).code.code, 404)
    }
  }

  test("forged ownership cannot grant access or transfer the stored resource") {
    withServer(OwnershipExample.server) { base =>
      val forged = """{"title":"Forged","ownerId":"bob"}"""
      assertEquals(request(base, "/documents/private", "PUT", Some("bob"), forged).code.code, 404)
      val owner = decoded[OwnedDocument](request(base, "/documents/private", "PUT", Some("alice"), forged))
      assertEquals(owner, OwnedDocument("private", "alice", "Forged"))
      assertEquals(request(base, "/documents/private", user = Some("bob")).code.code, 404)
    }
  }

  test("application identity mapping denies an unlinked provider identity") {
    withServer(OwnershipExample.server) { base =>
      assertEquals(request(base, "/documents/private", user = Some("unlinked")).code.code, 403)
      assertEquals(request(base, "/documents/private").code.code, 401)
      assertEquals(request(base, "/documents/private", user = Some("alice"), password = "wrong").code.code, 401)
    }
  }
