package ba.sake.sharaf.pac4j

import org.pac4j.core.adapter.FrameworkAdapter
import org.pac4j.core.config.Config

class Pac4jSecurityConfig(
    val pac4jConfig: Config,
    val clients: String = "",
    val authorizers: String = "",
    val matchers: String = "",
    val callbackPath: Option[String] = None,
    val logoutPath: Option[String] = None,
    val defaultLogoutUrl: String = "/",
) {
  // Configure pac4j to use Sharaf's context adapters (one-time setup, safe to share)
  pac4jConfig.setWebContextFactory(SharafPac4jContext.webContextFactory)
  pac4jConfig.setSessionStoreFactory(SharafPac4jContext.sessionStoreFactory)
  pac4jConfig.setHttpActionAdapter(SharafPac4jContext.globalHttpActionAdapter)
  FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(pac4jConfig)
}
