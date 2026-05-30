package ba.sake.sharaf.pac4j

import org.pac4j.core.config.Config
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.Path

final class Pac4jLogoutHandler(
    pac4jConfig: Config,
    defaultUrl: String = "/",
    logoutUrlPattern: String = "",
    localLogout: Boolean = true,
    destroySession: Boolean = true,
    centralLogout: Boolean = false,
) extends SharafHandler {

  override def handle(ctx: RequestContext): Response[?] = {
    val (method, path) = ctx.params
    val fullUrl = buildFullUrl(ctx.request, method, path)
    val frameworkParams = SharafFrameworkParameters(ctx.request, fullUrl, method)
    val sharafCtx = SharafPac4jContext(ctx.request, fullUrl, method.name)

    SharafPac4jContext.withCurrentContext(sharafCtx) {
      pac4jConfig.getLogoutLogic.perform(
        pac4jConfig, defaultUrl,
        if logoutUrlPattern.nonEmpty then logoutUrlPattern else null,
        Boolean.box(localLogout), Boolean.box(destroySession), Boolean.box(centralLogout),
        frameworkParams,
      )
    }

    val response = sharafCtx.toResponse()
    if response.status == sttp.model.StatusCode.InternalServerError then Response.redirect(defaultUrl)
    else response
  }

  private def buildFullUrl(req: Request, method: HttpMethod, path: Path): String = {
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
