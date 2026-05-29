package ba.sake.sharaf.pac4j

import sttp.model.*
import sttp.client4.quick.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.HeaderClient
import scala.compiletime.uninitialized
import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractSharafHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

class SecurityServiceTest extends AbstractSharafHandlerTest {

  val authHeader = "Authorization"

  val authClient = HeaderClient(authHeader, TestHeaderAuthenticator())
  authClient.setSaveProfileInSession(true)
  val clients = Clients(authClient)
  val config = Config(clients)
  val securityService = SecurityService(config)

  val appRoutes = Routes {
    case GET -> Path("whoami") =>
      val user = securityService.currentUser
      user match {
        case Some(u) => Response.withBody(u.getId)
        case None    => Response.withStatus(StatusCode.Unauthorized).withBody("anonymous")
      }
    case GET -> Path("protected") =>
      Response.withBody("secret data")
    case GET -> Path("hello") =>
      Response.withBody("hello")
  }

  private var server: JdkHttpServerSharafServer = uninitialized

  override def startServer(): Unit = {
    val securityConfig = Pac4jSecurityConfig(config)
    val handler = SharafHandler.sessions(
      SharafHandler.pac4j(SharafHandler.routes(appRoutes), securityConfig)
    )
    server = JdkHttpServerSharafServer("localhost", port, handler)
    server.start()
  }

  override def stopServer(): Unit = {
    server.stop()
  }

  test("currentUser returns None for unauthenticated request") {
    val res = quickRequest.get(uri"${baseUrl}/whoami").send()
    assertEquals(res.code, StatusCode.Unauthorized)
    assertEquals(res.body, "anonymous")
  }

  test("currentUser returns profile for authenticated request") {
    val res = quickRequest.get(uri"${baseUrl}/whoami")
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
