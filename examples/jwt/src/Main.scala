package jwt

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import org.pac4j.core.profile.BasicUserProfile
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.*
import ba.sake.sharaf.pac4j.*

@main def main(): Unit =
  val module = JwtModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class JwtModule(port: Int):

  val baseUrl = s"http://localhost:${port}"

  private val signatureConfiguration = new SecretSignatureConfiguration("your_jwt_secret_key_that_is_at_least_32_chars")
  private val jwtAuthenticator = JwtAuthenticator(signatureConfiguration)
  private val headerClient = new HeaderClient("Authorization", jwtAuthenticator)

  // generate a JWT claims set for testing purposes
  locally {
    val up = BasicUserProfile()
    up.setId("12345")
    val jwt = JwtGenerator(signatureConfiguration).generate(up)
    println(s"Use this JWT: ${jwt}")
  }

  private val clients = Clients("/callback", headerClient)
  private val pac4jConfig = Config(clients)

  // public route matcher
  private val publicRoutesMatcher = PathMatcher()
  private val publicRoutesMatcherName = "publicRoutesMatcher"
  publicRoutesMatcher.excludePaths("/")
  pac4jConfig.addMatcher(publicRoutesMatcherName, publicRoutesMatcher)

  // JWTs are stateless — no server-side session needed
  private val securityConfig = Pac4jSecurityConfig(
    pac4jConfig,
    matchers = s"${DefaultMatchers.SECURITYHEADERS},${publicRoutesMatcherName}",
    sessionStoreFactory = NoopSessionStore.factory,
  )

  private val routes = SharafHandler.pac4j(
    SharafHandler.routes(Routes {
      case GET -> Path() =>
        Response.withBody("Hello there! This is a public endpoint. Try accessing localhost:8181/protected.")
      case GET -> Path("protected") =>
        Response.withBody("This is a protected resource. You are authenticated.")
    }),
    securityConfig,
  )

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(BlockingHandler(SharafUndertowHandler(routes)))
    .build()
