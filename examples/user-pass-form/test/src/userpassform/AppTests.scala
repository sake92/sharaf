package userpassform

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.utils.*

class AppTests extends IntegrationTest {

  test("/protected-resource should return 302 redirect to /login-form when not logged in") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    val res = requests.get(s"$baseUrl/protected-resource", check = false, maxRedirects = 0)
    assertEquals(res.statusCode, 302)
    assertEquals(res.headers("location"), Seq("/login-form"))
  }

  test("/ and /form-login should return 200 when not logged in") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    assertEquals(requests.get(baseUrl).statusCode, 200)
    assertEquals(requests.get(s"$baseUrl/form-login").statusCode, 200)
  }

  test("/protected-resource should return 200 when logged in") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    val session = requests.Session()
    val loginRes = session.post(
      s"$baseUrl/callback?client_name=FormClient",
      data = LoginFormData("johndoe", "johndoe").toRequestsMultipart(),
      check = false,
      maxRedirects = 0
    )
    assertEquals(loginRes.statusCode, 303)
    val res = session.get(s"$baseUrl/protected-resource", check = false)
    assertEquals(res.statusCode, 200)
  }
}

case class LoginFormData(username: String, password: String) derives FormDataRW
