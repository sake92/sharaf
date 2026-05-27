package ba.sake.sharaf.pac4j

import java.util.Optional
import scala.jdk.CollectionConverters.*
import io.undertow.Undertow
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.BasicUserProfile
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
import sttp.client4.quick.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.*
import ba.sake.sharaf.utils.NetworkUtils

class Pac4jSupportTest extends munit.FunSuite {

  val port = NetworkUtils.getFreePort()
  val baseUrl = s"http://localhost:$port"

  val jwtSecret = "test_jwt_secret_key_that_is_at_least_32_chars"
  val signatureConfig = SecretSignatureConfiguration(jwtSecret)
  val jwtAuthenticator = JwtAuthenticator(signatureConfig)
  val headerClient = HeaderClient("Authorization", jwtAuthenticator)

  val clients = Clients("/callback", headerClient)
  val pac4jConfig = Config(clients)
  // JWT is stateless — no server-side session needed
  pac4jConfig.setSessionStoreFactory(_ => NoopSessionStore())

  val pac4jSupport = Pac4jSupport(pac4jConfig)

  val clientNames = clients.getClients.asScala.map(_.getName()).mkString(",")

  val routes = Routes {
    case GET -> Path("hello") =>
      val profile = pac4jSupport.currentProfile[BasicUserProfile]
      Response.withBody(s"hello ${profile.map(_.getId).getOrElse("anonymous")}")
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(
      pac4jSupport.securityHandler(
        SharafUndertowHandler(SharafHandler.routes(routes)),
        clientNames
      )
    )
    .build()

  // a valid JWT for testing
  val testProfile = BasicUserProfile()
  testProfile.setId("user-42")
  val validJwt = JwtGenerator(signatureConfig).generate(testProfile)

  override def beforeAll(): Unit = server.start()
  override def afterAll(): Unit = server.stop()

  test("returns 401 without credentials") {
    val res = quickRequest.get(uri"$baseUrl/hello").send()
    assertEquals(res.code.code, 401)
  }

  test("returns 200 and profile with valid JWT") {
    val res = quickRequest
      .get(uri"$baseUrl/hello")
      .header("Authorization", validJwt)
      .send()
    assertEquals(res.code.code, 200)
    assertEquals(res.body, "hello user-42")
  }
}

/** A no-op [[SessionStore]] that stores nothing; suitable for stateless direct clients (e.g. JWT). */
private class NoopSessionStore extends SessionStore {
  override def getTrackableSession(context: WebContext): Optional[AnyRef] = Optional.empty()
  override def buildFromTrackableSession(context: WebContext, trackableSession: Any): Optional[SessionStore] =
    Optional.empty()
  override def getSessionId(context: WebContext, createSession: Boolean): Optional[String] = Optional.empty()
  override def get(context: WebContext, key: String): Optional[AnyRef] = Optional.empty()
  override def set(context: WebContext, key: String, value: Any): Unit = ()
  override def destroySession(context: WebContext): Boolean = false
  override def renewSession(context: WebContext): Boolean = false
}
