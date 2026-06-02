package ba.sake.sharaf.pac4j

import scala.compiletime.uninitialized
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.testkit.JwtScenarios
import org.pac4j.testkit.TestConfigs
import ba.sake.sharaf.*
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer
import ba.sake.sharaf.utils.NetworkUtils

class JwtSelfTest extends munit.FunSuite:

  var server: Option[JdkHttpServerSharafServer] = None
  var serverUrl: String = uninitialized

  override def beforeAll(): Unit =
    this.getClass.getClassLoader.setDefaultAssertionStatus(true)

    val port = NetworkUtils.getFreePort()
    serverUrl = s"http://localhost:$port"

    val pac4jConfig = TestConfigs.jwtConfig()
    // Fix for pac4j 6.5.2: JwtProfileDefinition no longer has setRolesClaimName.
    // Override convertAndAdd to extract "roles" claim into profile roles.
    val headerClient = pac4jConfig.getClients.findClient("HeaderClient").get
      .asInstanceOf[org.pac4j.http.client.direct.HeaderClient]
    val jwtAuth = headerClient.getAuthenticator.asInstanceOf[JwtAuthenticator]
    jwtAuth.setProfileDefinition(new RoleExtractingJwtProfileDefinition)

    // /protected: auth only, no authorizer
    val userSecurityConfig = Pac4jSecurityConfig(pac4jConfig, clients = "HeaderClient")
    val userRoutes = Routes {
      case GET -> Path("protected") => Response.withBody("OK")
    }
    val userHandler = Pac4jSecurityHandler(userSecurityConfig, SharafHandler.routes(userRoutes))

    // /protected/admin: auth + admin role authorizer
    val adminSecurityConfig = Pac4jSecurityConfig(
      pac4jConfig,
      clients = "HeaderClient",
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

  test("happy path") { JwtScenarios.runHappyPath(serverUrl) }
  test("missing token") { JwtScenarios.runMissingToken(serverUrl) }
  test("invalid token") { JwtScenarios.runInvalidToken(serverUrl) }
  test("expired token") { JwtScenarios.runExpiredToken(serverUrl) }
  test("admin access") { JwtScenarios.runAdminAccess(serverUrl) }
  test("admin denied") { JwtScenarios.runAdminDenied(serverUrl) }

/** JwtProfileDefinition for pac4j 6.5.2 that extracts the "roles" claim into profile roles.
  * In pac4j 6.1.0 setRolesClaimName did this; pac4j 6.5.2 removed it.
  */
private class RoleExtractingJwtProfileDefinition extends org.pac4j.jwt.profile.JwtProfileDefinition:
  override def convertAndAdd(
      profile: org.pac4j.core.profile.UserProfile,
      location: org.pac4j.core.profile.AttributeLocation,
      name: String,
      value: Object,
  ): Unit =
    super.convertAndAdd(profile, location, name, value)
    // Extract "roles" claim into profile roles for RequireAnyRoleAuthorizer
    if name == "roles" && value.isInstanceOf[java.util.List[?]] then
      import scala.jdk.CollectionConverters.*
      val rolesList = value.asInstanceOf[java.util.List[String]]
      profile.addRoles(rolesList)

