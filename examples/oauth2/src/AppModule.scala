package demo

import ba.sake.sharaf.*
import ba.sake.sharaf.handlers.*
import ba.sake.sharaf.routing.*

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

class AppModule(clients: Clients) {

  private val securityConfig = SecurityConfig(clients)
  private val securityService = new SecurityService(securityConfig.pac4jConfig)
  private val appRoutes = new AppRoutes(securityService)

  private val httpHandler: HttpHandler = locally {
    val securityHandler =
      SecurityHandler.build(
        ErrorHandler(
          RoutesHandler(appRoutes.routes)
        ),
        securityConfig.pac4jConfig,
        securityConfig.clientNames.mkString(","),
        null,
        securityConfig.matchers,
        new CustomSecurityLogic()
      )

    val pathHandler = Handlers
      .path()
      .addExactPath(
        "/callback",
        CallbackHandler.build(securityConfig.pac4jConfig, null, new CustomCallbackLogic())
      )
      .addExactPath("/logout", new LogoutHandler(securityConfig.pac4jConfig, "/"))
      .addPrefixPath("/", securityHandler)

    new SessionAttachmentHandler(
      pathHandler,
      new InMemorySessionManager("SessionManager"),
      new SessionCookieConfig()
    )
  }

  val server = Undertow
    .builder()
    .addHttpListener(8181, "0.0.0.0", httpHandler)
    .build()
}
