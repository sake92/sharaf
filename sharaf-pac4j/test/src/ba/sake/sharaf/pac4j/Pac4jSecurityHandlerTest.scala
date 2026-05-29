package ba.sake.sharaf.pac4j

import sttp.model.*
import sttp.client4.quick.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.HttpConstants
import org.pac4j.http.client.direct.HeaderClient
import scala.compiletime.uninitialized
import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.AbstractSharafHandlerTest
import ba.sake.sharaf.jdkhttp.JdkHttpServerSharafServer

class Pac4jSecurityHandlerTest extends AbstractSharafHandlerTest {

  val authHeader = "Authorization"

  val authClient = HeaderClient(authHeader, TestHeaderAuthenticator())
  authClient.setSaveProfileInSession(true)
  val clients = Clients(authClient)
  val config = Config(clients)
  val securityConfig = Pac4jSecurityConfig(config)

  val appRoutes = Routes {
    case GET -> Path("public") =>
      Response.withBody("public data")
    case GET -> Path("protected") =>
      Response.withBody("secret data")
  }

  private var server: JdkHttpServerSharafServer = uninitialized

  override def startServer(): Unit = {
    val handler = SharafHandler.sessions(
      SharafHandler.pac4j(SharafHandler.routes(appRoutes), securityConfig)
    )
    server = JdkHttpServerSharafServer("localhost", port, handler)
    server.start()
  }

  override def stopServer(): Unit = {
    server.stop()
  }

  test("public route is accessible without authentication") {
    val res = quickRequest.get(uri"${baseUrl}/public").send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "public data")
  }

  test("protected route returns 401 without authentication") {
    val res = quickRequest.get(uri"${baseUrl}/protected").send()
    assertEquals(res.code.code, HttpConstants.UNAUTHORIZED)
  }

  test("protected route returns 200 with valid auth header") {
    val res = quickRequest.get(uri"${baseUrl}/protected")
      .header(authHeader, "user123")
      .send()
    assertEquals(res.code, StatusCode.Ok)
    assertEquals(res.body, "secret data")
  }
}
