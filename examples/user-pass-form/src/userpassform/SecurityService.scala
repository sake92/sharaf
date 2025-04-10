package userpassform

import scala.jdk.OptionConverters.*
import org.pac4j.core.config.Config
import org.pac4j.core.util.FindBest
import org.pac4j.undertow.context.{UndertowSessionStore, UndertowWebContext}
import ba.sake.sharaf.Request

class SecurityService(config: Config) {

  def currentUser(using req: Request): Option[CustomUserProfile] = {
    val exchange = req.underlyingHttpServerExchange
    @annotation.nowarn
    val sessionStore = FindBest.sessionStore(null, config, UndertowSessionStore(exchange))
    val profileManager = config.getProfileManagerFactory.apply(UndertowWebContext(exchange), sessionStore)
    profileManager.getProfile().toScala.map { profile =>
      CustomUserProfile(profile.getUsername)
    }
  }

  def getCurrentUser(using req: Request): CustomUserProfile =
    currentUser.getOrElse(throw NotAuthenticatedException())
}

case class CustomUserProfile(name: String)

class NotAuthenticatedException extends RuntimeException
