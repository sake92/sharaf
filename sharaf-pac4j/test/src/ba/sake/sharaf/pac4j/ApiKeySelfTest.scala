package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.testkit.ApiKeyScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

class ApiKeySelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)


    val pac4jConfig = TestConfigs.directConfig()
    // Fix for pac4j 6.5.2: SimpleTestTokenAuthenticator only accepts "test-api-key"
    val client = pac4jConfig.getClients.findClient("ApiKeyClient").get.asInstanceOf[HeaderClient]
    client.setAuthenticator(SharafTestAuthenticators.apiKey)

    val securityConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "ApiKeyClient",
    )

    val routes = Routes {
      case GET -> Path("protected") =>
        Response.withBody("OK")
    }

    val handler = Pac4jSecurityHandler(securityConfig, SharafHandler.routes(routes))
    val s = JdkHttpServerSharafServer("localhost", handler)
    s.start()
    serverUrl = s"http://localhost:${s.port}"
    server = Some(s)

  override def afterAll(): Unit =
    server.foreach(_.stop())

  test("happy path") { ApiKeyScenarios.runHappyPath(serverUrl) }
  test("bad key") { ApiKeyScenarios.runBadKey(serverUrl) }
  test("missing key") { ApiKeyScenarios.runMissingKey(serverUrl) }
