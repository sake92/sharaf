package ba.sake.sharaf.pac4j

import io.undertow.server.HttpHandler
import org.pac4j.core.config.Config
import org.pac4j.core.profile.UserProfile
import org.pac4j.undertow.handler.{CallbackHandler, LogoutHandler, SecurityHandler}
import org.pac4j.undertow.context.{UndertowParameters, UndertowWebContext}
import ba.sake.sharaf.Request
import ba.sake.sharaf.undertow.UndertowSharafRequest

import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*

/** Provides pac4j security integration for sharaf/Undertow applications.
  *
  * @param config
  *   pac4j [[Config]] holding clients, authorizers, matchers, etc.
  */
class Pac4jSupport(val config: Config):

  /** Returns the primary (first) profile of the authenticated user, if any.
    *
    * Cast to `P` is unchecked at compile time; a [[ClassCastException]] will be thrown at runtime if the profile type
    * does not match.
    */
  def currentProfile[P <: UserProfile](using req: Request): Option[P] =
    val exchange = req.asInstanceOf[UndertowSharafRequest].underlyingHttpServerExchange
    val sessionStore = config.getSessionStoreFactory.newSessionStore(UndertowParameters(exchange))
    val profileManager = config.getProfileManagerFactory.apply(UndertowWebContext(exchange), sessionStore)
    profileManager.getProfile().toScala.map(_.asInstanceOf[P])

  /** Returns all profiles of the authenticated user (useful when multiple clients are used simultaneously). */
  def currentProfiles[P <: UserProfile](using req: Request): List[P] =
    val exchange = req.asInstanceOf[UndertowSharafRequest].underlyingHttpServerExchange
    val sessionStore = config.getSessionStoreFactory.newSessionStore(UndertowParameters(exchange))
    val profileManager = config.getProfileManagerFactory.apply(UndertowWebContext(exchange), sessionStore)
    profileManager.getProfiles().asScala.map(_.asInstanceOf[P]).toList

  /** Wraps an existing [[HttpHandler]] with pac4j security enforcement.
    *
    * @param handler
    *   The inner handler to protect.
    * @param clients
    *   Comma-separated client names (or empty string / null for all).
    * @param authorizers
    *   Comma-separated authorizer names (or empty string / null for defaults).
    * @param matchers
    *   Comma-separated matcher names (or empty string / null for defaults).
    */
  def securityHandler(
      handler: HttpHandler,
      clients: String = null,
      authorizers: String = null,
      matchers: String = null
  ): HttpHandler =
    SecurityHandler.build(handler, config, clients, authorizers, matchers)

  /** Builds a pac4j callback handler for indirect (redirect-based) clients.
    *
    * @param defaultUrl
    *   URL to redirect to after a successful callback (null means pac4j default).
    */
  def callbackHandler(defaultUrl: String = null): HttpHandler =
    CallbackHandler.build(config, defaultUrl)

  /** Builds a pac4j logout handler.
    *
    * @param redirectTo
    *   URL to redirect to after logout.
    */
  def logoutHandler(redirectTo: String = "/"): HttpHandler =
    LogoutHandler(config, redirectTo)
