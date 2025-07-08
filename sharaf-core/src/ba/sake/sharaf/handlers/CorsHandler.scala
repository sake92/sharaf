package ba.sake.sharaf.handlers

import scala.jdk.CollectionConverters.*
import ba.sake.sharaf.*
import sttp.model.HeaderNames

// https://www.moesif.com/blog/technical/cors/Authoritative-Guide-to-CORS-Cross-Origin-Resource-Sharing-for-REST-APIs/
final class CorsHandler(corsSettings: CorsSettings, next: SharafHandler) extends SharafHandler {

  override def handle(context: RequestContext): Response[?] =
    if context.params._1 == HttpMethod.OPTIONS then {
      setCorsHeaders(context.request, Response.default)
        .settingHeader(
          HeaderNames.AccessControlAllowMethods,
          corsSettings.allowedHttpMethods.map(_.toString).mkString(", ")
        )
        .settingHeader(
          HeaderNames.AccessControlAllowHeaders,
          corsSettings.allowedHttpHeaders.map(_.toString).mkString(", ")
        )
    } else {
      val res = next.handle(context)
      setCorsHeaders(context.request, res)
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
    var tmpRes = res.settingHeader(HeaderNames.AccessControlAllowCredentials, corsSettings.allowCredentials.toString)
    allowOpt.foreach { allow =>
      tmpRes = tmpRes.settingHeader(HeaderNames.AccessControlAllowOrigin, allow)
    }
    tmpRes
  }
}
