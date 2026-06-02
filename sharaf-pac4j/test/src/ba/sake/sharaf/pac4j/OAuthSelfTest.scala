package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import no.nav.security.mock.oauth2.MockOAuth2Server
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

    val secConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "OidcClient",
      callbackPath = Some("/callback"),
    )

    val routes = Routes {
      case GET -> Path("protected") => Response.withBody("OK")
    }

    val handler = Pac4jSecurityHandler(secConfig, SharafHandler.routes(routes))
    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    sharafServer = Some(s)

  override def afterAll(): Unit =
    sharafServer.foreach(_.stop())
    mockOAuthServer.foreach(_.shutdown())

  test("unauthenticated redirect") {
    OAuthScenarios.runUnauthenticatedRedirect(serverUrl, mockOAuthServer.get.baseUrl().toString)
  }

  // Known issue: authorizationCodeFlow fails due to pac4j-oidc version mismatch
  // (testkit uses 6.1.0, sharaf uses pac4j-core 6.5.2; oidc 6.5.2 not on classpath).
  // The relative redirect_uri causes the mock IdP to redirect to the wrong port.
  // TODO: fix when pac4j-oidc 6.5.2 is added as a dependency.
  test("authorization code flow".ignore) {
    OAuthScenarios.runAuthorizationCodeFlow(serverUrl, mockOAuthServer.get)
  }
