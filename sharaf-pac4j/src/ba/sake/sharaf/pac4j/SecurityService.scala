package ba.sake.sharaf.pac4j

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.util.Base64
import scala.util.Using
import scala.jdk.CollectionConverters.*
import org.pac4j.core.profile.UserProfile
import ba.sake.sharaf.session.Session

/** Convenience helper for accessing pac4j user profiles from Sharaf routes.
  *
  * Profiles are automatically saved to the Sharaf session by [[Pac4jSecurityHandler]]
  * when authentication succeeds. Use [[currentUser]] to access them in your routes.
  *
  * Example:
  * {{{
  * case GET -> Path("whoami") =>
  *   val user = SecurityService.currentUser
  *   Response.withBody(s"Hello, ${user.map(_.getId).getOrElse("anonymous")}")
  * }}}
  */
object SecurityService:

  /** Returns all currently authenticated user profiles. */
  def profiles: List[UserProfile] =
    try
      Session.current.getOpt[String]("pac4j.pac4jUserProfiles") match
        case Some(encoded) => deserializeProfiles(encoded)
        case None          => List.empty
    catch
      case _: Exception => List.empty

  /** Returns the first authenticated user profile, if any. */
  def currentUser: Option[UserProfile] = profiles.headOption

  private def deserializeProfiles(encoded: String): List[UserProfile] =
    try
      val bytes = Base64.getDecoder.decode(encoded)
      Using(new ByteArrayInputStream(bytes)) { bais =>
        Using(new ObjectInputStream(bais)) { ois =>
          ois.readObject().asInstanceOf[java.util.LinkedHashMap[String, UserProfile]]
        }.get
      }.get.values().asScala.toList
    catch
      case _: Exception => List.empty
