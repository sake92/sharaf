package ba.sake.sharaf.pac4j

import org.pac4j.core.profile.UserProfile

/** Convenience helper for accessing pac4j user profiles from Sharaf routes.
  *
  * Profiles are set by [[Pac4jSecurityHandler]] on each authenticated request.
  * Use [[currentUser]] to access them in your routes.
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
    Option(Pac4jSecurityHandler.currentProfiles.get()).toList.flatten

  /** Returns the first authenticated user profile, if any. */
  def currentUser: Option[UserProfile] = profiles.headOption
