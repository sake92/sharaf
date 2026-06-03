package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.testkit.AuthorizerScenarios
import org.pac4j.testkit.TestConfigs
import org.pac4j.testkit.TestConstants
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class PathMatcherSelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    val pac4jConfig = TestConfigs.pathMatcherConfig()
    // Fix for pac4j 6.5.2: extract "roles" claim into profile roles
    val headerClient = pac4jConfig.getClients.findClient("HeaderClient").get
      .asInstanceOf[org.pac4j.http.client.direct.HeaderClient]
    val jwtAuth = headerClient.getAuthenticator.asInstanceOf[JwtAuthenticator]
    jwtAuth.setProfileDefinition(new RoleExtractingJwtProfileDefinition)

    // /protected: auth with PathMatcher (excludes /other from security)
    val securityConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "HeaderClient",
      matchers = "excludedPath",
    )
    val protectedHandler = Pac4jSecurityHandler(securityConfig, SharafHandler.routes(Routes {
      case GET -> Path("protected") => Response.withBody("OK")
    }))

    // /other: public, no security
    val publicHandler = SharafHandler.routes(Routes {
      case GET -> Path("other") => Response.withBody("OK")
    })

    // Combined handler dispatches by path prefix
    val handler = new SharafHandler {
      override def handle(ctx: RequestContext): Response[?] =
        val pathStr = "/" + ctx.params._2.segments.mkString("/")
        if pathStr == TestConstants.UNPROTECTED_PATH then publicHandler.handle(ctx)
        else protectedHandler.handle(ctx)
    }

    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    server = Some(s)

  override def afterAll(): Unit =
    server.foreach(_.stop())

  test("path matcher protected") { AuthorizerScenarios.runPathMatcherProtected(serverUrl) }
  test("path matcher other public") { AuthorizerScenarios.runPathMatcherOtherPublic(serverUrl) }
