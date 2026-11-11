package ba.sake.sharaf.handlers

import sttp.model.HeaderNames
import ba.sake.sharaf.*

// https://www.moesif.com/blog/technical/cors/Authoritative-Guide-to-CORS-Cross-Origin-Resource-Sharing-for-REST-APIs/
final class CorsHandler(corsSettings: CorsSettings, next: SharafHandler) extends SharafHandler {

  override def handle(context: RequestContext): Response[?] = {
    val path = "/" + context.params._2.segments.mkString("/")
    val applies = corsSettings.pathPrefixes.exists(path.startsWith)
    val headers = context.request.headers
    val preflight = context.params._1 == HttpMethod.OPTIONS &&
      headers.get(HttpString(HeaderNames.Origin)).exists(_.nonEmpty) &&
      headers.get(HttpString(HeaderNames.AccessControlRequestMethod)).exists(_.nonEmpty)
    if !applies then next.handle(context)
    else if preflight then {
      setCorsHeaders(context.request, Response.default)
        .settingHeader(
          HeaderNames.AccessControlAllowMethods,
          corsSettings.allowedHttpMethods.map(_.toString).mkString(", ")
        )
        .settingHeader(
          HeaderNames.AccessControlAllowHeaders,
          corsSettings.allowedHttpHeaders.map(_.toString).mkString(", ")
        )
        .settingHeader(HeaderNames.AccessControlMaxAge, corsSettings.preflightMaxAge.getSeconds.toString)
    } else {
      val res = next.handle(context)
      setCorsHeaders(context.request, res)
    }
  }

  private def setCorsHeaders[T](req: Request, res: Response[T]): Response[T] = {
    val allowOpt =
      if corsSettings.allowedOrigins.contains("*") then Some("*")
      else
        req.headers.get(HttpString(HeaderNames.Origin)) match {
          case Some(origins) =>
            Option.when(corsSettings.allowedOrigins(origins.head))(origins.head)
          case _ =>
            None // noop
        }
    val bodyHeaders = res.body.toSeq.flatMap(res.rw.headers)
    val currentVary = res.headerUpdates.resolve(bodyHeaders).collectFirst {
      case HeaderUpdate.Set(name, values) if name == HttpString(HeaderNames.Vary) => values
    }.getOrElse(Seq.empty)
    val varyTokens = currentVary.flatMap(_.split(",")).map(_.trim)
    val needsVary = !corsSettings.allowedOrigins.contains("*") &&
      !varyTokens.exists(value => value == "*" || value.equalsIgnoreCase("Origin"))
    val withVary = if needsVary then res.settingHeader(HeaderNames.Vary, currentVary :+ "Origin") else res
    var tmpRes = withVary.settingHeader(HeaderNames.AccessControlAllowCredentials, corsSettings.allowCredentials.toString)
    allowOpt.foreach { allow =>
      tmpRes = tmpRes.settingHeader(HeaderNames.AccessControlAllowOrigin, allow)
    }
    tmpRes
  }
}
