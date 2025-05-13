package userpassform

import sttp.model.*
import sttp.client4.quick.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*

class AppTests extends IntegrationTest {

  test("/protected-resource should return 302 redirect to /login-form when not logged in") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    val res = quickRequest.get(uri"$baseUrl/protected-resource").followRedirects(false).send()
    assertEquals(res.code, StatusCode.Found)
    assertEquals(res.headers(HeaderNames.Location), Seq("/login-form"))
  }

  test("/ and /form-login should return 200 when not logged in") {
    val module = moduleFixture()
    val baseUrl = Uri.apply(module.baseUrl)
    assertEquals(quickRequest.get(baseUrl).send().code, StatusCode.Ok)
    assertEquals(quickRequest.get(uri"$baseUrl/form-login").send().code, StatusCode.Ok)
  }

  test("/protected-resource should return 200 when logged in") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    val cookieHandler = new java.net.CookieManager()
    val javaClient = java.net.http.HttpClient.newBuilder().cookieHandler(cookieHandler).build()
    val statefulBackend = sttp.client4.httpclient.HttpClientSyncBackend.usingClient(javaClient)
    val loginRes = quickRequest
      .get(uri"$baseUrl/callback?client_name=FormClient")
      .multipartBody(LoginFormData("johndoe", "johndoe").toSttpMultipart())
      .followRedirects(false)
      .send(statefulBackend)

    assertEquals(loginRes.code, StatusCode.SeeOther)
    val res = quickRequest.get(uri"$baseUrl/protected-resource").send(statefulBackend)
    assertEquals(res.code, StatusCode.Ok)
  }
}

case class LoginFormData(username: String, password: String) derives FormDataRW
