package ba.sake.sharaf.pac4j

import io.undertow.Handlers
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.session.{InMemorySessionManager, SessionAttachmentHandler, SessionCookieConfig}
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.SharafUndertowHandler

/** Undertow server with pac4j security wired in.
  *
  * Handles the session attachment, path routing for callback/logout, and security wrapping automatically. Use this when
  * your application uses **indirect clients** (e.g. OAuth2, SAML, CAS) that require server-side sessions and
  * redirect-based flows.
  *
  * For **direct clients** only (e.g. JWT, HTTP-Basic), you can use [[UndertowSharafServer]] and apply
  * [[Pac4jSupport.securityHandler]] manually — no sessions are required.
  *
  * @param host
  *   Hostname to listen on.
  * @param port
  *   Port to listen on.
  * @param routes
  *   Application routes to protect.
  * @param pac4jSupport
  *   Configured [[Pac4jSupport]] instance.
  * @param clients
  *   Comma-separated client names to enforce (null/empty = all clients).
  * @param authorizers
  *   Comma-separated authorizer names (null/empty = pac4j defaults).
  * @param matchers
  *   Comma-separated matcher names (null/empty = pac4j defaults).
  * @param callbackUrl
  *   Path for the pac4j callback endpoint (default: "/callback").
  * @param logoutUrl
  *   Path for the logout endpoint (default: "/logout").
  * @param logoutRedirectTo
  *   URL to redirect to after logout (default: "/").
  * @param sessionManagerName
  *   Name passed to [[InMemorySessionManager]] (default: "SharafSessionManager").
  * @param corsSettings
  *   CORS settings for the application routes.
  * @param exceptionMapper
  *   Exception mapper for the application routes.
  * @param notFoundHandler
  *   Handler invoked when no route matches.
  */
class Pac4jUndertowServer(
    host: String,
    port: Int,
    routes: Routes,
    pac4jSupport: Pac4jSupport,
    clients: String = null,
    authorizers: String = null,
    matchers: String = null,
    callbackUrl: String = "/callback",
    logoutUrl: String = "/logout",
    logoutRedirectTo: String = "/",
    sessionManagerName: String = "SharafSessionManager",
    corsSettings: CorsSettings = CorsSettings.default,
    exceptionMapper: ExceptionMapper = ExceptionMapper.default,
    notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
):

  private val httpHandler: HttpHandler =
    val cpResHandler = SharafHandler.classpathResources(
      "public",
      SharafHandler.classpathResources("META-INF/resources/webjars", notFoundHandler)
    )
    val innerHandler = SharafUndertowHandler(
      SharafHandler.exceptions(
        SharafHandler.cors(
          SharafHandler.routes(routes, cpResHandler),
          corsSettings
        ),
        exceptionMapper
      )
    )

    val securedHandler = pac4jSupport.securityHandler(innerHandler, clients, authorizers, matchers)

    val pathHandler = Handlers
      .path()
      .addExactPath(callbackUrl, pac4jSupport.callbackHandler())
      .addExactPath(logoutUrl, pac4jSupport.logoutHandler(logoutRedirectTo))
      .addPrefixPath("/", securedHandler)

    BlockingHandler(
      SessionAttachmentHandler(
        pathHandler,
        InMemorySessionManager(sessionManagerName),
        SessionCookieConfig()
      )
    )

  private val server = Undertow
    .builder()
    .addHttpListener(port, host)
    .setHandler(httpHandler)
    .build()

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()
