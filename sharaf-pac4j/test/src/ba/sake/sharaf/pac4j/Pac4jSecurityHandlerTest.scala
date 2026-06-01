package ba.sake.sharaf.pac4j

import java.util.Optional
import sttp.model.*
import sttp.client4.quick.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.{CallContext, HttpConstants as Pac4jHttpConstants}
import org.pac4j.core.credentials.{Credentials, TokenCredentials}
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http.client.direct.HeaderClient
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class Pac4jSecurityHandlerTest extends munit.FunSuite:

  val port: Int = NetworkUtils.getFreePort()
  def baseUrl: String = s"http://localhost:${port}"

  val authHeader = "Authorization"
  val authClient = HeaderClient(authHeader, TestHeaderAuthenticator())
  authClient.setName("HeaderClient")
  authClient.setSaveProfileInSession(true)

  val clients = Clients(authClient)
  val pac4jConfig = Config(clients)

  val publicMatcherName = "publicRoutes"
  val publicMatcher = PathMatcher()
  publicMatcher.excludePaths("/public")
  pac4jConfig.addMatcher(publicMatcherName, publicMatcher)

  val securityConfig = Pac4jSecurityConfig(
    pac4jConfig,
    clients = "HeaderClient",
    matchers = s"${DefaultMatchers.SECURITYHEADERS},$publicMatcherName",
  )

  val appRoutes = Routes {
    case GET -> Path("public")    => Response.withBody("public data")
    case GET -> Path("protected") => Response.withBody("secret data")
  }

  val handler = Pac4jSecurityHandler(securityConfig, SharafHandler.routes(appRoutes))
  var server: Option[JdkHttpServerSharafServer] = None

  override def beforeAll(): Unit =
    val s = JdkHttpServerSharafServer("localhost", port, handler)
    s.start()
    server = Some(s)

  override def afterAll(): Unit =
    server.foreach(_.stop())

  test("public route is accessible without authentication"):
    val res = quickRequest.get(uri"${baseUrl}/public").send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "public data")

  test("protected route returns 401 without authentication"):
    val res = quickRequest.get(uri"${baseUrl}/protected").send()
    assertEquals(res.code.code, Pac4jHttpConstants.UNAUTHORIZED)

  test("protected route returns 200 with valid auth header"):
    val res = quickRequest.get(uri"${baseUrl}/protected")
      .header(authHeader, "user123")
      .send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "secret data")

class TestHeaderAuthenticator extends Authenticator:

  override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
    credentials match
      case tc: TokenCredentials =>
        val token = tc.getToken
        if token != null && token.nonEmpty then
          val profile = new CommonProfile()
          profile.setId(token)
          profile.addAttribute("username", token)
          tc.setUserProfile(profile)
          Optional.of(tc)
        else Optional.empty()
      case _ => Optional.empty()
