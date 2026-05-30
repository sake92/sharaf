package ba.sake.sharaf.pac4j

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

    val (method, path) = ctx.params
    val fullUrl = buildFullUrl(ctx.request, path)
    val frameworkParams = SharafFrameworkParameters(ctx.request, fullUrl, method)
    val sharafCtx = SharafPac4jContext(ctx.request, fullUrl, method.name)

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

    val clientsParam = Option(securityConfig.clients).filter(_.nonEmpty).orNull
    val authorizersParam = Option(securityConfig.authorizers).filter(_.nonEmpty).orNull
    val matchersParam = Option(securityConfig.matchers).filter(_.nonEmpty).orNull

    SharafPac4jContext.withCurrentContext(sharafCtx) {
      pac4jConfig.getSecurityLogic.perform(
        pac4jConfig, successAdapter,
        clientsParam, authorizersParam, matchersParam,
        frameworkParams,
      )
    }

    sharafCtx.toResponse()
  }

  private def buildFullUrl(req: Request, path: Path): String = {
    val host = req.headers.get(HttpString("Host")).flatMap(_.headOption).getOrElse("localhost")
    val pathStr = "/" + path.segments.mkString("/")
    val query = {
      val raw = req.queryParamsRaw
      if raw.isEmpty then ""
      else "?" + raw.flatMap { (k, vs) => vs.map(v => java.net.URLEncoder.encode(k, "UTF-8") + "=" + java.net.URLEncoder.encode(v, "UTF-8")) }.mkString("&")
    }
    s"http://$host$pathStr$query"
  }
}
