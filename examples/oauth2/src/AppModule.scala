package demo

import io.undertow.Handlers
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.session.InMemorySessionManager
import io.undertow.server.session.SessionAttachmentHandler
import io.undertow.server.session.SessionCookieConfig
import io.undertow.server.handlers.BlockingHandler
import org.pac4j.core.client.Clients
import org.pac4j.undertow.handler.CallbackHandler
import org.pac4j.undertow.handler.LogoutHandler
import org.pac4j.undertow.handler.SecurityHandler
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.SharafUndertowHandler

class AppModule(port: Int, clients: Clients) {

  val baseUrl = s"http://localhost:${port}"

  private val securityConfig = SecurityConfig(clients)
  private val securityService = SecurityService(securityConfig.pac4jConfig)
  private val appRoutes = AppRoutes(securityService)

  private val httpHandler: HttpHandler = locally {
    val securityHandler =
      SecurityHandler.build(
        SharafUndertowHandler(
          SharafHandler.exceptions(
            SharafHandler.routes(appRoutes.routes)
          )
        ),
        securityConfig.pac4jConfig,
        securityConfig.clientNames.mkString(","),
        null,
        securityConfig.matchers
      )

    val pathHandler = Handlers
      .path()
      .addExactPath("/callback", CallbackHandler.build(securityConfig.pac4jConfig))
      .addExactPath("/logout", LogoutHandler(securityConfig.pac4jConfig, "/"))
      .addPrefixPath("/", securityHandler)

    BlockingHandler(
      SessionAttachmentHandler(pathHandler, InMemorySessionManager("SessionManager"), SessionCookieConfig())
    )
  }

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost", httpHandler)
    .build()
}
