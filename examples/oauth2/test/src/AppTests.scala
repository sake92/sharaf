package demo

import sttp.model.*
import sttp.client4.quick.*

class AppTests extends IntegrationTest {

  test("/protected should return 302 Found when not logged in") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl
    val res = quickRequest.get(uri"$baseUrl/protected").followRedirects(false).send()
    assertEquals(res.code, StatusCode.Found)
  }

  test("/protected should return 200 Ok when logged in") {
    val module = moduleFixture()
    val baseUrl = module.baseUrl

    // we use a stateful backend to keep the session cookie!
    val cookieHandler = new java.net.CookieManager()
    val javaClient = java.net.http.HttpClient.newBuilder().cookieHandler(cookieHandler).build()
    val statefulBackend = sttp.client4.httpclient.HttpClientSyncBackend.usingClient(javaClient)
    // this does OAuth2 ping-pong redirects etc,
    // and we get a JSESSSIONID cookie
    quickRequest.get(uri"$baseUrl/login?provider=GenericOAuth20Client").send(statefulBackend)

    val res = quickRequest.get(uri"$baseUrl/protected").followRedirects(false).send(statefulBackend)
    assertEquals(res.code, StatusCode.Ok)
  }
}
