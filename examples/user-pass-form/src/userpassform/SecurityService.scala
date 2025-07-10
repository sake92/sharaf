package userpassform

import scala.jdk.OptionConverters.*
import org.pac4j.core.config.Config
import org.pac4j.undertow.context.{UndertowParameters, UndertowWebContext}
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafRequest

class SecurityService(config: Config) {

  def currentUser(using req: Request): Option[CustomUserProfile] = {
    val exchange = req.asInstanceOf[UndertowSharafRequest].underlyingHttpServerExchange
    val sessionStore = config.getSessionStoreFactory.newSessionStore(UndertowParameters(exchange))
    val profileManager = config.getProfileManagerFactory.apply(UndertowWebContext(exchange), sessionStore)
    profileManager.getProfile().toScala.map { profile =>
      CustomUserProfile(profile.getUsername)
    }
  }

  def getCurrentUser(using req: Request): CustomUserProfile =
    currentUser.getOrElse(throw NotAuthenticatedException())

  // convenient utility method so that you don't have to pass the user around
  def withCurrentUser[T](f: CustomUserProfile ?=> T)(using req: Request): T = {
    f(using getCurrentUser)
  }
}

class NotAuthenticatedException extends RuntimeException
