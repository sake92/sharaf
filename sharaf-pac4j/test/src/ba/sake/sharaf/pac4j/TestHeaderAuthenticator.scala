package ba.sake.sharaf.pac4j

import java.util.Optional
import org.pac4j.core.context.CallContext
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.credentials.TokenCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.profile.CommonProfile

class TestHeaderAuthenticator extends Authenticator {

  override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] = {
    credentials match {
      case tc: TokenCredentials =>
        val token = tc.getToken
        if token != null && token.nonEmpty then {
          val profile = new CommonProfile()
          profile.setId(token)
          profile.addAttribute("username", token)
          tc.setUserProfile(profile)
          Optional.of(tc)
        } else {
          Optional.empty()
        }
      case _ => Optional.empty()
    }
  }
}
