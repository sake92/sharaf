package jwt

import java.util.Optional
import scala.jdk.CollectionConverters.*
import io.undertow.Undertow
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.undertow.handler.SecurityHandler
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import org.pac4j.core.profile.BasicUserProfile
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.*

// TODO add a test

@main def main(): Unit =
  val module = JwtModule(8181)
  module.server.start()
  println(s"Started HTTP server at ${module.baseUrl}")

class JwtModule(port: Int) {

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
  // use noop session store, JWTs are stateless
  pac4jConfig.setSessionStoreFactory(_ => NoopSessionStore())
  private val clientNames = clients.getClients.asScala.map(_.getName()).toSeq
  private val publicRoutesMatcher = PathMatcher()
  private val publicRoutesMatcherName = "publicRoutesMatcher"
  publicRoutesMatcher.excludePaths("/")
  pac4jConfig.addMatcher(publicRoutesMatcherName, publicRoutesMatcher)
  private val securityHandler =
    SecurityHandler.build(
      UndertowExceptionHandler(
        ExceptionMapper.default,
        SharafUndertowHandler(SharafHandler.routes(Routes {
          case GET -> Path() =>
            Response.withBody("Hello there! This is a public endpoint. Try accessing localhost:8181/protected.")
          case GET -> Path("protected") =>
            Response.withBody("This is a protected resource. You are authenticated.")
        }))
      ),
      pac4jConfig,
      clientNames.mkString(","),
      null,
      s"${DefaultMatchers.SECURITYHEADERS},${publicRoutesMatcherName}"
    )

  val server = Undertow
    .builder()
    .addHttpListener(port, "localhost")
    .setHandler(securityHandler)
    .build()
}

// A no-op session store implementation that does not store any session data
class NoopSessionStore extends SessionStore {
  override def getTrackableSession(context: WebContext): Optional[AnyRef] = Optional.empty()

  override def buildFromTrackableSession(context: WebContext, trackableSession: Any): Optional[SessionStore] =
    Optional.empty()

  override def getSessionId(context: WebContext, createSession: Boolean): Optional[String] = Optional.empty()

  override def get(context: WebContext, key: String): Optional[AnyRef] = Optional.empty()

  override def set(context: WebContext, key: String, value: Any): Unit = ()

  override def destroySession(context: WebContext): Boolean = false

  override def renewSession(context: WebContext): Boolean = false
}
