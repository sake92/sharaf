package ba.sake.sharaf.pac4j

import ba.sake.sharaf.{SharafHandler, RequestContext, Response, HttpString}

/** Handles the OAuth/OIDC callback endpoint. */
final class Pac4jCallbackHandler(securityConfig: Pac4jSecurityConfig) extends SharafHandler:

  override def handle(context: RequestContext): Response[?] =
    val (method, path) = context.params
    val fullUrl = buildFullUrl(context.request, path)
    val webContext = new SharafWebContext(context.request, fullUrl, method)
    val pac4jConfig = securityConfig.pac4jConfig

    val frameworkParams = new SharafFrameworkParameters(context.request, fullUrl, method)

    val result = pac4jConfig.getCallbackLogic.perform(
      pac4jConfig,
      securityConfig.callbackPath.orNull,
      true, // renewSession
      null, // defaultClient
      frameworkParams
    )

    result match
      case res: Response[?] => webContext.supplementResponse(res)
      case _ =>
        webContext.supplementResponse(
          Response.redirect(securityConfig.callbackPath.getOrElse("/"))
        )

  private def buildFullUrl(req: ba.sake.sharaf.Request, path: ba.sake.sharaf.routing.Path): String =
    val host = req.headers.get(HttpString("Host")).flatMap(_.headOption).getOrElse("localhost")
    val pathStr = "/" + path.segments.mkString("/")
    s"http://$host$pathStr"
