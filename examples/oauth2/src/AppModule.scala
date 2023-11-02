package demo

import io.undertow.Handlers
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.session.InMemorySessionManager
import io.undertow.server.session.SessionAttachmentHandler
import io.undertow.server.session.SessionCookieConfig
import org.pac4j.core.client.Clients
import org.pac4j.undertow.handler.CallbackHandler
import org.pac4j.undertow.handler.LogoutHandler
import org.pac4j.undertow.handler.SecurityHandler
import ba.sake.sharaf.*

class AppModule(port: Int, clients: Clients) {

  val baseUrl = s"http://localhost:${port}"

  private val securityConfig = SecurityConfig(clients)
  private val securityService = SecurityService(securityConfig.pac4jConfig)
  private val appRoutes = AppRoutes(securityService)

  private val httpHandler: HttpHandler = locally {
    val securityHandler =
      SecurityHandler.build(
        SharafHandler(appRoutes.routes),
        securityConfig.pac4jConfig,
        securityConfig.clientNames.mkString(","),
        null,
        securityConfig.matchers,
        CustomSecurityLogic()
      )

    val pathHandler = Handlers
      .path()
      .addExactPath(
        "/callback",
        CallbackHandler.build(securityConfig.pac4jConfig, null, CustomCallbackLogic())
      )
      .addExactPath("/logout", LogoutHandler(securityConfig.pac4jConfig, "/"))
      .addPrefixPath("/", securityHandler)

    SessionAttachmentHandler(
      pathHandler,
      InMemorySessionManager("SessionManager"),
      SessionCookieConfig()
    )
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "0.0.0.0", httpHandler)
    .build()
}
