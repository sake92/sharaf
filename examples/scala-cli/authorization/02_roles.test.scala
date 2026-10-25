package authorization

import authorization.roles.RolesExample

class RolesSuite extends HttpExampleSuite:
  test("any-role access permits either role and combined roles") {
    withServer(RolesExample.server) { base =>
      for user <- List("admin", "support", "both") do
        val response = request(base, "/reports", user = Some(user))
        assertEquals(response.code.code, 200)
        assertEquals(response.body, "Report")
      assertEquals(request(base, "/reports", user = Some("reader")).code.code, 403)
      assertEquals(request(base, "/reports").code.code, 401)
      assertEquals(request(base, "/reports", user = Some("admin"), password = "wrong").code.code, 401)
    }
  }

  test("empty role set denies even an administrator and still authenticates") {
    withServer(RolesExample.server) { base =>
      assertEquals(request(base, "/closed", user = Some("admin")).code.code, 403)
      assertEquals(request(base, "/closed", user = Some("reader")).code.code, 403)
      assertEquals(request(base, "/closed").code.code, 401)
    }
  }

  test("only matching routes run role checks after authentication") {
    withServer(RolesExample.server) { base =>
      assertEquals(request(base, "/unknown", user = Some("reader")).code.code, 404)
      assertEquals(request(base, "/unknown").code.code, 401)
      assertEquals(request(base, "/reports/child", user = Some("reader")).code.code, 404)
      assertEquals(request(base, "/reports", user = Some("reader")).code.code, 403)
    }
  }
