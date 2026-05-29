package ba.sake.sharaf.pac4j

import org.pac4j.core.adapter.FrameworkAdapter
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.engine.SecurityGrantedAccessAdapter
import org.pac4j.core.profile.UserProfile
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.Path

final class Pac4jSecurityHandler(
    securityConfig: Pac4jSecurityConfig,
    wrapped: SharafHandler,
) extends SharafHandler {

  override def handle(ctx: RequestContext): Response[?] = {
    val pac4jConfig = securityConfig.pac4jConfig

    pac4jConfig.setWebContextFactory(SharafPac4jContext.webContextFactory)
    pac4jConfig.setSessionStoreFactory(SharafPac4jContext.sessionStoreFactory)
    FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(pac4jConfig)

    val (method, path) = ctx.params
    val fullUrl = buildFullUrl(ctx.request, path)
    val frameworkParams = SharafFrameworkParameters(ctx.request, fullUrl, method)
    val sharafCtx = SharafPac4jContext(ctx.request, fullUrl, method.name)
    pac4jConfig.setHttpActionAdapter(SharafPac4jContext.httpActionAdapterFor(sharafCtx))

    val successAdapter = new SecurityGrantedAccessAdapter {
      override def adapt(
          webContext: WebContext,
          sessionStore: SessionStore,
          profiles: java.util.Collection[UserProfile],
      ): AnyRef = {
        try {
          sharafCtx.successResponse = Some(wrapped.handle(ctx))
          null
        } catch {
          case e: Exception => throw RuntimeException(e)
        }
      }
    }

    pac4jConfig.getSecurityLogic.perform(
      pac4jConfig, successAdapter,
      securityConfig.clients, securityConfig.authorizers, securityConfig.matchers,
      frameworkParams,
    )

    sharafCtx.toResponse()
  }

  private def buildFullUrl(req: Request, path: Path): String = {
    val host = req.headers.get(HttpString("Host")).flatMap(_.headOption).getOrElse("localhost")
    val pathStr = "/" + path.segments.mkString("/")
    val query = {
      val raw = req.queryParamsRaw
      if raw.isEmpty then ""
      else "?" + raw.flatMap { (k, vs) => vs.map(v => s"$k=$v") }.mkString("&")
    }
    s"http://$host$pathStr$query"
  }
}
