package demo

import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.DefaultCallbackLogic
import org.pac4j.core.profile.UserProfile
import org.pac4j.oauth.profile.github.GitHubProfile
import org.pac4j.oauth.profile.OAuth20Profile

class CustomCallbackLogic() extends DefaultCallbackLogic {

  override def saveUserProfile(
      context: WebContext,
      sessionStore: SessionStore,
      config: Config,
      userProfile: UserProfile,
      saveProfileInSession: Boolean,
      multiProfile: Boolean,
      renewSession: Boolean
  ): Unit = {
    super.saveUserProfile(context, sessionStore, config, userProfile, saveProfileInSession, multiProfile, renewSession)

    userProfile match
      case profile: GitHubProfile =>
        // save to database etc. whatever is needed
        println(s"Saving profile to database: $profile")
      case profile: OAuth20Profile =>
        // this should probably be a different CallbackLogic for tests..
        println(s"Saving TEST profile to database: $profile")
      case other =>
        throw RuntimeException(s"Cant handle Pac4jUserProfile: $other")

  }
}
