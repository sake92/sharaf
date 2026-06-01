package userpassform

import scala.jdk.CollectionConverters.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.credentials.password.JBCryptPasswordEncoder
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.definition.CommonProfileDefinition
import org.pac4j.core.profile.factory.ProfileFactory
import org.pac4j.core.profile.service.InMemoryProfileService
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.http.client.indirect.FormClient
import ba.sake.sharaf.*
import ba.sake.sharaf.pac4j.*
import ba.sake.sharaf.undertow.*

@main def main: Unit =
  val module = UserPassFormModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class UserPassFormModule(port: Int):

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
  private val clientNames = clients.getClients.asScala.map(_.getName()).toSeq

  // exclude public paths from security (callback & logout are handled by Pac4jSecurityHandler)
  private val publicRoutesMatcher = PathMatcher()
  publicRoutesMatcher.excludePaths("/", "/login-form", callbackUrl, "/logout")
  pac4jConfig.addMatcher("publicRoutes", publicRoutesMatcher)

  private val securityConfig = Pac4jSecurityConfig(
    pac4jConfig,
    clients = clientNames.mkString(","),
    matchers = s"${DefaultMatchers.SECURITYHEADERS},publicRoutes",
  )
    .withCallbackPath(callbackUrl)
    .withLogoutPath("/logout")

  private val appRoutes = new AppRoutes(callbackUrl)

  val server = UndertowSharafServer(
    "localhost",
    port,
    Pac4jSecurityHandler(securityConfig, SharafHandler.routes(appRoutes.routes))
  )
