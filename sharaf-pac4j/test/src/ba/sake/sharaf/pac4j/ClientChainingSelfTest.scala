package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.testkit.ClientChainingScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class ClientChainingSelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    val pac4jConfig = TestConfigs.chainedConfig()
    // Fix for pac4j 6.5.2: extract "roles" claim into profile roles
    val headerClient = pac4jConfig.getClients.findClient("HeaderClient").get
      .asInstanceOf[org.pac4j.http.client.direct.HeaderClient]
    val jwtAuth = headerClient.getAuthenticator.asInstanceOf[JwtAuthenticator]
    jwtAuth.setProfileDefinition(new RoleExtractingJwtProfileDefinition)
    // Fix API key client authenticator
    val apiKeyClient = pac4jConfig.getClients.findClient("ApiKeyClient").get
      .asInstanceOf[org.pac4j.http.client.direct.HeaderClient]
    apiKeyClient.setAuthenticator(SharafTestAuthenticators.apiKey)

    // User-level: /protected (auth only)
    val userSecurityConfig = Pac4jSecurityConfig(pac4jConfig, clients = "HeaderClient,ApiKeyClient")
    val userRoutes = Routes {
      case GET -> Path("protected") => Response.withBody("OK")
    }
    val userHandler = Pac4jSecurityHandler(userSecurityConfig, SharafHandler.routes(userRoutes))

    // Admin-level: /protected/admin (auth + admin role)
    val adminSecurityConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "HeaderClient,ApiKeyClient",
      authorizers = "admin",
    )
    val adminRoutes = Routes {
      case GET -> Path("protected", "admin") => Response.withBody("ADMIN OK")
    }
    val adminHandler = Pac4jSecurityHandler(adminSecurityConfig, SharafHandler.routes(adminRoutes))

    // Combined handler dispatches by path prefix
    val handler = new SharafHandler {
      override def handle(ctx: RequestContext): Response[?] =
        val pathStr = "/" + ctx.params._2.segments.mkString("/")
        if pathStr == "/protected/admin" then adminHandler.handle(ctx)
        else userHandler.handle(ctx)
    }

    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    server = Some(s)

  override def afterAll(): Unit =
    server.foreach(_.stop())

  test("jwt succeeds") { ClientChainingScenarios.runJwtSucceeds(serverUrl) }
  test("api key succeeds") { ClientChainingScenarios.runApiKeySucceeds(serverUrl) }
  test("neither fails") { ClientChainingScenarios.runNeitherFails(serverUrl) }
  test("jwt priority over api key") { ClientChainingScenarios.runJwtPriorityOverApiKey(serverUrl) }
  test("jwt valid api key invalid") { ClientChainingScenarios.runJwtValidApiKeyInvalid(serverUrl) }
  test("jwt expired api key valid") { ClientChainingScenarios.runJwtExpiredApiKeyValid(serverUrl) }
