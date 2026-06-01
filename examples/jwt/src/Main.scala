package jwt

import java.util.Optional
import scala.jdk.CollectionConverters.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import org.pac4j.core.profile.BasicUserProfile
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
import ba.sake.sharaf.*
import ba.sake.sharaf.session.NoOpSessionStore
import ba.sake.sharaf.pac4j.*
import ba.sake.sharaf.undertow.*

@main def main(): Unit =
  val module = JwtModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class JwtModule(port: Int):

  val baseUrl = s"http://localhost:${port}"

  private val signatureConfiguration =
    new SecretSignatureConfiguration("your_jwt_secret_key_that_is_at_least_32_chars")
  private val jwtAuthenticator = JwtAuthenticator(signatureConfiguration)
  private val headerClient = new HeaderClient("Authorization", jwtAuthenticator)
  headerClient.setName("HeaderClient")

  // generate a JWT for testing purposes
  locally {
    val up = BasicUserProfile()
    up.setId("12345")
    val jwt = JwtGenerator(signatureConfiguration).generate(up)
    println(s"Use this JWT: $jwt")
  }

  private val clients = Clients("/callback", headerClient)
  private val pac4jConfig = Config(clients)

  // exclude root path from authentication
  private val publicRoutesMatcher = PathMatcher()
  publicRoutesMatcher.excludePaths("/")
  pac4jConfig.addMatcher("publicRoutes", publicRoutesMatcher)

  private val securityConfig = Pac4jSecurityConfig(
    pac4jConfig,
    clients = "HeaderClient",
    matchers = s"${DefaultMatchers.SECURITYHEADERS},publicRoutes",
    sessionStore = NoOpSessionStore.instance, // stateless — JWT is self-contained
  )

  private val appRoutes = Routes {
    case GET -> Path() =>
      Response.withBody(
        "Hello there! This is a public endpoint. Try accessing localhost:8181/protected."
      )
    case GET -> Path("protected") =>
      Response.withBody("This is a protected resource. You are authenticated.")
  }

  val server = UndertowSharafServer(
    "localhost",
    port,
    Pac4jSecurityHandler(securityConfig, SharafHandler.routes(appRoutes))
  )
