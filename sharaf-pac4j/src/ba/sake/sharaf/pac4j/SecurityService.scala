package ba.sake.sharaf.pac4j

import scala.jdk.OptionConverters.*
import org.pac4j.core.config.Config
import org.pac4j.core.profile.{ProfileManager, UserProfile}
import ba.sake.sharaf.*

class SecurityService(pac4jConfig: Config) {

  def currentUser(using req: Request): Option[UserProfile] = {
    val fullUrl = {
      val host = req.headers.get(HttpString("Host")).flatMap(_.headOption).getOrElse("localhost")
      s"http://$host/"
    }
    val sharafCtx = SharafPac4jContext(req, fullUrl, "GET")
    val profileManager = ProfileManager(sharafCtx, sharafCtx)
    profileManager.getProfile.toScala
  }

  def getCurrentUser(using req: Request): UserProfile =
    currentUser.getOrElse(throw NotAuthenticatedException())

  def withCurrentUser[T](f: UserProfile ?=> T)(using req: Request): T =
    f(using getCurrentUser)
}

class NotAuthenticatedException extends RuntimeException("User not authenticated")
