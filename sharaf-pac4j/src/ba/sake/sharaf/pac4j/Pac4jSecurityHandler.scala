package ba.sake.sharaf.pac4j

import java.util.Collection as JCollection
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore as Pac4jSessionStore
import org.pac4j.core.engine.SecurityGrantedAccessAdapter
import org.pac4j.core.profile.UserProfile
import ba.sake.sharaf.{SharafHandler, RequestContext, Response, Cookie, HttpString}
import ba.sake.sharaf.routing.Path
import ba.sake.sharaf.session.{SessionImpl, SessionHolder, NoOpSessionStore}

/** A [[SharafHandler]] decorator that applies pac4j security AND manages session lifecycle.
  *
  * Subsumes both session management and pac4j security in one handler.
  */
final class Pac4jSecurityHandler(
    securityConfig: Pac4jSecurityConfig,
    next: SharafHandler
) extends SharafHandler:

  override def handle(context: RequestContext): Response[?] =
    val (method, path) = context.params
    val fullUrl = buildFullUrl(context.request, path)
    val webContext = new SharafWebContext(context.request, fullUrl, method)
    val pac4jConfig = securityConfig.pac4jConfig

    try
      val frameworkParams = new SharafFrameworkParameters(context.request, fullUrl, method)

      val accessAdapter = new SecurityGrantedAccessAdapter:
        override def adapt(
            wc: WebContext,
            sessionStore: Pac4jSessionStore,
            profiles: JCollection[UserProfile]
        ): AnyRef =
          val res = next.handle(context)
          finalizeResponse(webContext, res)

      val result = pac4jConfig.getSecurityLogic.perform(
        pac4jConfig,
        accessAdapter,
        securityConfig.clients,
        securityConfig.authorizers,
        securityConfig.matchers,
        frameworkParams
      )

      result match
        case res: Response[?] => res
        case _ =>
          webContext.supplementResponse(Response.default)
    finally
      SessionHolder.clear()

  private def buildFullUrl(req: ba.sake.sharaf.Request, path: Path): String =
    val host = req.headers.get(HttpString("Host")).flatMap(_.headOption).getOrElse("localhost")
    val pathStr = "/" + path.segments.mkString("/")
    val query = {
      val raw = req.queryParamsRaw
      if raw.isEmpty then ""
      else "?" + raw.flatMap { (k, vs) =>
        vs.map(v => java.net.URLEncoder.encode(k, "UTF-8") + "=" + java.net.URLEncoder.encode(v, "UTF-8"))
      }.mkString("&")
    }
    s"http://$host$pathStr$query"

  private def finalizeResponse(
      webContext: SharafWebContext,
      res: Response[?]
  ): Response[?] =
    val supplemented = webContext.supplementResponse(res)

    val session = SessionHolder.get
    session match
      case Some(s) =>
        securityConfig.sessionStore match
          case _: NoOpSessionStore => supplemented
          case store =>
            store.save(s.asInstanceOf[SessionImpl])
            supplemented.settingCookie(
              Cookie(
                name = "SHARAF_SESSION",
                value = s.id,
                path = Some("/"),
                maxAge = Some(1800),
                secure = true,
                httpOnly = true,
                sameSite = true,
                sameSiteMode = Some("Strict")
              )
            )
      case None => supplemented
