package authorization

import authorization.authentication.AuthenticationExample

class AuthenticationSuite extends HttpExampleSuite:
  test("authentication boundaries, challenge, and sequential identities") {
    withServer(AuthenticationExample.server) { base =>
      assertEquals(request(base, "/").body, "Public")
      assertEquals(request(base, "/").code.code, 200)
      for user <- List("alice", "bob") do
        val allowed = request(base, "/members", user = Some(user))
        assertEquals(allowed.code.code, 200)
        assertEquals(allowed.body, "Members only")
        assertEquals(allowed.header("Set-Cookie"), None)
        val anonymous = request(base, "/members")
        assertEquals(anonymous.code.code, 401)
        assert(anonymous.header("WWW-Authenticate").exists(_.startsWith("Basic")))
        assertEquals(request(base, "/members", user = Some(user), password = "wrong").code.code, 401)
      assertEquals(request(base, "/unknown").code.code, 401)
      assertEquals(request(base, "/unknown", user = Some("alice")).code.code, 404)
    }
  }

  test("path matcher exempts public paths without exempting neighboring prefixes") {
    withServer(AuthenticationExample.server) { base =>
      assertEquals(request(base, "/public/help").code.code, 200)
      assertEquals(request(base, "/public/help").body, "Public help")
      assertEquals(request(base, "/public/missing").code.code, 404)
      assertEquals(request(base, "/publicity").code.code, 401)
      assertEquals(request(base, "/members/child").code.code, 401)
    }
  }
