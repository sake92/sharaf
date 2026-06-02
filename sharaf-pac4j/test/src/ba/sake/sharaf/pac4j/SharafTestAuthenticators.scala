package ba.sake.sharaf.pac4j

import java.util.Optional
import org.pac4j.core.context.CallContext
import org.pac4j.core.credentials.{Credentials, TokenCredentials, UsernamePasswordCredentials}
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.profile.CommonProfile

/** Authenticator helpers compatible with pac4j 6.5.2.
  *
  * In pac4j 6.1.0, SimpleTestUsernamePasswordAuthenticator accepted any non-empty credentials.
  * In 6.5.2, it requires username == password. These helpers restore the 6.1.0 behavior for the
  * testkit scenarios which expect testuser/testpass and admin/adminpass.
  */
object SharafTestAuthenticators:

  /** Accepts testuser/testpass and admin/adminpass only. Rejects everything else. */
  val usernamePassword: Authenticator = new Authenticator {
    override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
      credentials match
        case upc: UsernamePasswordCredentials =>
          val username = upc.getUsername
          val password = upc.getPassword
          val validCreds = (username == "testuser" && password == "testpass") ||
                           (username == "admin" && password == "adminpass")
          if validCreds then
            val profile = new CommonProfile()
            profile.setId(username)
            upc.setUserProfile(profile)
            Optional.of(upc)
          else Optional.empty()
        case _ => Optional.empty()
  }

  /** Accepts only the exact token "test-api-key" (matching TestConstants.API_KEY). */
  val apiKey: Authenticator = new Authenticator {
    override def validate(ctx: CallContext, credentials: Credentials): Optional[Credentials] =
      credentials match
        case tc: TokenCredentials =>
          val token = tc.getToken
          if token == "test-api-key" then
            val profile = new CommonProfile()
            profile.setId(token)
            tc.setUserProfile(profile)
            Optional.of(tc)
          else Optional.empty()
        case _ => Optional.empty()
  }
