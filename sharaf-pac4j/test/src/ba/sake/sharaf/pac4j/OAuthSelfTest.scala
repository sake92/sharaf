package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.pac4j.oidc.client.OidcClient
import org.pac4j.testkit.OAuthScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class OAuthSelfTest extends munit.FunSuite:

  var mockOAuthServer: Option[MockOAuth2Server] = None
  var sharafServer: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val mockServer = new MockOAuth2Server()
    mockServer.start()
    mockOAuthServer = Some(mockServer)
    val mockServerUrl = mockServer.baseUrl().toString

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    val pac4jConfig = TestConfigs.oauthConfig(mockServerUrl)
    val oidcClient = pac4jConfig.getClients.findClient("OidcClient").get.asInstanceOf[OidcClient]
    oidcClient.setCallbackUrl(s"http://localhost:$port/callback")

    val secConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "OidcClient",
      callbackPath = Some("/callback"),
    )

    val handler = SharafHandler.sessions(
      Pac4jSecurityHandler(secConfig, SharafHandler.routes(Routes {
        case GET -> Path("protected") => Response.withBody("OK")
      }))
    )

    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    sharafServer = Some(s)

  override def afterAll(): Unit =
    sharafServer.foreach(_.stop())
    mockOAuthServer.foreach(_.shutdown())

  test("unauthenticated redirect") {
    OAuthScenarios.runUnauthenticatedRedirect(serverUrl, mockOAuthServer.get.baseUrl().toString)
  }

  // Known issue: mock-oauth2-server 4.0.0 produces tokens with typ=test-client-id header.
  // pac4j-oidc 6.5.2 (via nimbus DefaultJOSEObjectTypeVerifier) rejects non-standard typ values.
  // This worked in pac4j 6.1.0 but the testkit was compiled against 6.1.0 and we run against 6.5.2.
  // Fix: requires either a mock-oauth2-server update or custom OidcProfileCreator with lenient type checking.
  test("authorization code flow".ignore) {
    OAuthScenarios.runAuthorizationCodeFlow(serverUrl, mockOAuthServer.get)
  }
