package ba.sake.sharaf.pac4j

import sttp.model.*
import sttp.client4.quick.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import org.pac4j.http.client.direct.HeaderClient
import scala.compiletime.uninitialized
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class SecurityServiceTest extends munit.FunSuite {

  val port: Int = NetworkUtils.getFreePort()
  def baseUrl: String = s"http://localhost:${port}"

  val authHeader = "Authorization"
  val authClient = HeaderClient(authHeader, TestHeaderAuthenticator())
  authClient.setSaveProfileInSession(true)

  val clients = Clients(authClient)
  val pac4jConfig = Config(clients)

  val publicMatcherName = "publicRoutes"
  val publicMatcher = PathMatcher()
  publicMatcher.excludePaths("/public", "/public-whoami")
  pac4jConfig.addMatcher(publicMatcherName, publicMatcher)

  val securityService = SecurityService(pac4jConfig)

  val securityConfig = Pac4jSecurityConfig(
    pac4jConfig,
    matchers = s"${DefaultMatchers.SECURITYHEADERS},$publicMatcherName",
  )

  val appRoutes = Routes {
    case GET -> Path("public-whoami") =>
      // Public route: excluded from security. currentUser will be None unless session had a profile from a previous auth.
      Response.withBody(securityService.currentUser.map(_.getId).getOrElse("anonymous"))
    case GET -> Path("protected-whoami") =>
      // Protected route: pac4j auth required. After auth, SecurityService finds the profile.
      Response.withBody(securityService.currentUser.map(_.getId).getOrElse("anonymous"))
    case GET -> Path("protected") =>
      Response.withBody("secret data")
  }

  private var server: JdkHttpServerSharafServer = uninitialized

  override def beforeAll(): Unit = {
    val handler = SharafHandler.sessions(
      SharafHandler.pac4j(SharafHandler.routes(appRoutes), securityConfig)
    )
    server = JdkHttpServerSharafServer("localhost", port, handler)
    server.start()
  }

  override def afterAll(): Unit = {
    server.stop()
  }

  test("currentUser returns 'anonymous' on public route without previous auth") {
    val res = quickRequest.get(uri"${baseUrl}/public-whoami").send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "anonymous")
  }

  test("currentUser returns profile on protected route with auth") {
    val res = quickRequest.get(uri"${baseUrl}/protected-whoami")
      .header(authHeader, "john_doe")
      .send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "john_doe")
  }

  test("protected route works with auth") {
    val res = quickRequest.get(uri"${baseUrl}/protected")
      .header(authHeader, "john_doe")
      .send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "secret data")
  }
}
