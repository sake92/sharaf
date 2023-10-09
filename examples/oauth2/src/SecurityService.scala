package demo

import scala.jdk.OptionConverters.*
import org.pac4j.core.config.Config
import org.pac4j.core.util.FindBest
import org.pac4j.undertow.context.{UndertowSessionStore, UndertowWebContext}
import ba.sake.sharaf.Request

class SecurityService(config: Config) {

  def currentUser(using req: Request): Option[CustomUserProfile] = {
    val exchange = req.underlyingHttpServerExchange

    @annotation.nowarn
    val sessionStore = FindBest.sessionStore(null, config, new UndertowSessionStore(exchange))

    val profileManager = config.getProfileManagerFactory().apply(new UndertowWebContext(exchange), sessionStore)

    profileManager.getProfile().toScala.map { profile =>
      // val identityProvider = profile match ..
      // val identityProviderId = profile.getId()
      // find it in db by type+id for example

      CustomUserProfile(profile.getUsername())
    }
  }

  def getCurrentUser(using req: Request): CustomUserProfile =
    currentUser.getOrElse(throw NotAuthenticatedException())
}

case class CustomUserProfile(name: String)

class NotAuthenticatedException extends RuntimeException
