package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.core.client.Clients
import org.pac4j.http.client.direct.DirectBasicAuthClient
import org.pac4j.testkit.BasicScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class BasicSelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    // Don't use TestConfigs.basicConfig() — it creates IndirectBasicAuthClient which works
    // differently in pac4j 6.5.2. Create our own config with DirectBasicAuthClient.
    val client = new DirectBasicAuthClient(SharafTestAuthenticators.usernamePassword)
    client.setName("DirectBasicAuthClient")
    val clients = new Clients("/callback", client)
    val pac4jConfig = new org.pac4j.core.config.Config(clients)

    val securityConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "DirectBasicAuthClient",
    )

    val routes = Routes {
      case GET -> Path("protected")          => Response.withBody("OK")
      case GET -> Path("protected", "admin") => Response.withBody("ADMIN OK")
    }

    val handler = Pac4jSecurityHandler(securityConfig, SharafHandler.routes(routes))
    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    server = Some(s)

  override def afterAll(): Unit =
    server.foreach(_.stop())

  test("happy path") { BasicScenarios.runHappyPath(serverUrl) }
  test("bad credentials") { BasicScenarios.runBadCredentials(serverUrl) }
  test("missing auth") { BasicScenarios.runMissingAuth(serverUrl) }
