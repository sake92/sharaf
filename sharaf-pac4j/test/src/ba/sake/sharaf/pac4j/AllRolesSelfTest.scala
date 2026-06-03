package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.testkit.AuthorizerScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class AllRolesSelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    val pac4jConfig = TestConfigs.allRolesAuthorizerConfig()
    // Fix for pac4j 6.5.2: extract "roles" claim into profile roles
    val headerClient = pac4jConfig.getClients.findClient("HeaderClient").get
      .asInstanceOf[org.pac4j.http.client.direct.HeaderClient]
    val jwtAuth = headerClient.getAuthenticator.asInstanceOf[JwtAuthenticator]
    jwtAuth.setProfileDefinition(new RoleExtractingJwtProfileDefinition)

    // /protected: auth with RequireAllRoles (ROLE_USER AND ROLE_ADMIN)
    val securityConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "HeaderClient",
      authorizers = "userAndAdmin",
    )
    val routes = Routes {
      case GET -> Path("protected") => Response.withBody("OK")
    }
    val handler = Pac4jSecurityHandler(securityConfig, SharafHandler.routes(routes))

    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    server = Some(s)

  override def afterAll(): Unit =
    server.foreach(_.stop())

  test("require all roles access granted") { AuthorizerScenarios.runRequireAllRolesAccessGranted(serverUrl) }
  test("require all roles access denied") { AuthorizerScenarios.runRequireAllRolesAccessDenied(serverUrl) }
