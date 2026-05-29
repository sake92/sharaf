package ba.sake.sharaf.pac4j

import org.pac4j.core.config.Config

class Pac4jSecurityConfig(
    val pac4jConfig: Config,
    val clients: String = "",
    val authorizers: String = "",
    val matchers: String = "",
    val callbackPath: Option[String] = None,
    val logoutPath: Option[String] = None,
    val defaultLogoutUrl: String = "/",
)
