package userpassform

import scala.jdk.CollectionConverters.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.handlers.SharafHandler
import io.undertow.server.session.{InMemorySessionManager, SessionAttachmentHandler, SessionCookieConfig}
import io.undertow.{Handlers, Undertow}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.credentials.password.JBCryptPasswordEncoder
import org.pac4j.core.engine.{DefaultCallbackLogic, DefaultSecurityLogic}
import org.pac4j.core.matching.matcher.*
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.definition.CommonProfileDefinition
import org.pac4j.core.profile.factory.ProfileFactory
import org.pac4j.core.profile.service.InMemoryProfileService
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.undertow.handler.{CallbackHandler, LogoutHandler, SecurityHandler}

@main def main: Unit =
  val module = UserPassFormModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class UserPassFormModule(port: Int) {

  val baseUrl = s"http://localhost:${port}"

  // just a dummy user store
  private val profileService = locally {
    val profileFactory: ProfileFactory = _ => new CommonProfile()
    val service = new InMemoryProfileService(profileFactory)
    service.setPasswordEncoder(new JBCryptPasswordEncoder())
    val profile1 = new CommonProfile()
    profile1.setId("user1")
    profile1.addAttribute(Pac4jConstants.USERNAME, "johndoe")
    profile1.addAttribute(CommonProfileDefinition.FIRST_NAME, "John")
    profile1.addAttribute(CommonProfileDefinition.FAMILY_NAME, "Doe")
    service.create(profile1, "johndoe")
    service
  }

  private val callbackUrl = "/callback"
  private val formClient = new FormClient("/login-form", profileService)
  private val clients = Clients(callbackUrl, formClient)
  private val pac4jConfig = Config(clients)
  private val publicRoutesMatcher = PathMatcher()
  private val publicRoutesMatcherName = "publicRoutesMatcher"
  publicRoutesMatcher.excludePaths("/", "/login-form")
  pac4jConfig.addMatcher(publicRoutesMatcherName, publicRoutesMatcher)
  private val clientNames = clients.getClients.asScala.map(_.getName()).toSeq
  val securityService = SecurityService(pac4jConfig)
  private val securityHandler =
    SecurityHandler.build(
      SharafHandler(AppRoutes(callbackUrl, securityService).routes),
      pac4jConfig,
      clientNames.mkString(","),
      null,
      s"${DefaultMatchers.SECURITYHEADERS},${publicRoutesMatcherName}",
      DefaultSecurityLogic()
    )
  private val pathHandler = Handlers
    .path()
    .addExactPath(callbackUrl, CallbackHandler.build(pac4jConfig, null, DefaultCallbackLogic()))
    .addExactPath("/logout", LogoutHandler(pac4jConfig, "/"))
    .addPrefixPath("/", securityHandler)

  private val finalHandler =
    SessionAttachmentHandler(pathHandler, InMemorySessionManager("SessionManager"), SessionCookieConfig())

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(finalHandler)
    .build()
}
