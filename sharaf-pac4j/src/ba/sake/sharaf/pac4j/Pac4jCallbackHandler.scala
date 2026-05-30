package ba.sake.sharaf.pac4j

import org.pac4j.core.config.Config
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.Path

final class Pac4jCallbackHandler(
    pac4jConfig: Config,
    defaultUrl: String = "/",
    renewSession: Boolean = false,
    defaultClient: String = "",
) extends SharafHandler {

  override def handle(ctx: RequestContext): Response[?] = {
    val (method, path) = ctx.params
    val fullUrl = buildFullUrl(ctx.request, method, path)
    val frameworkParams = SharafFrameworkParameters(ctx.request, fullUrl, method)
    val sharafCtx = SharafPac4jContext(ctx.request, fullUrl, method.name)

    val clientName = if defaultClient.nonEmpty then defaultClient
    else pac4jConfig.getClients.getClients.get(0).getName

    SharafPac4jContext.withCurrentContext(sharafCtx) {
      pac4jConfig.getCallbackLogic.perform(
        pac4jConfig, defaultUrl, Boolean.box(renewSession), clientName, frameworkParams,
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
