package ba.sake.sharaf.pac4j

import org.pac4j.core.config.Config
import org.pac4j.core.adapter.FrameworkAdapter
import ba.sake.sharaf.session.SessionStore

/** Wraps a pac4j [[Config]] with Sharaf adapters pre-wired.
  *
  * @param pac4jConfig   The pac4j Config to wrap. Sharaf factories are applied on construction.
  * @param clients       Comma-separated client names (e.g. "HeaderClient,OAuthClient")
  * @param authorizers   Comma-separated authorizer names
  * @param matchers      Comma-separated matcher names
  * @param callbackPath  Optional OAuth callback path
  * @param logoutPath    Optional logout path
  * @param sessionStore  Sharaf session store for non-stateless auth
  */
final class Pac4jSecurityConfig(
    val pac4jConfig: Config,
    val clients: String = "",
    val authorizers: String = "",
    val matchers: String = "",
    val callbackPath: Option[String] = None,
    val logoutPath: Option[String] = None,
    val defaultLogoutUrl: String = "/",
    val sessionStore: SessionStore = ba.sake.sharaf.session.InMemorySessionStore(),
):

  // Apply Sharaf adapters
  pac4jConfig.setWebContextFactory(SharafWebContext.factory)
  pac4jConfig.setSessionStoreFactory(SharafSessionStore.factory(sessionStore))
  pac4jConfig.setHttpActionAdapter(SharafHttpActionAdapter())
  FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(pac4jConfig)

  def withSessionStore(store: SessionStore): Pac4jSecurityConfig =
    pac4jConfig.setSessionStoreFactory(SharafSessionStore.factory(store))
    new Pac4jSecurityConfig(pac4jConfig, clients, authorizers, matchers,
      callbackPath, logoutPath, defaultLogoutUrl, store)

  def withCallbackPath(path: String): Pac4jSecurityConfig =
    new Pac4jSecurityConfig(pac4jConfig, clients, authorizers, matchers,
      Some(path), logoutPath, defaultLogoutUrl, sessionStore)

  def withLogoutPath(path: String): Pac4jSecurityConfig =
    new Pac4jSecurityConfig(pac4jConfig, clients, authorizers, matchers,
      callbackPath, Some(path), defaultLogoutUrl, sessionStore)

  def withDefaultLogoutUrl(url: String): Pac4jSecurityConfig =
    new Pac4jSecurityConfig(pac4jConfig, clients, authorizers, matchers,
      callbackPath, logoutPath, url, sessionStore)
