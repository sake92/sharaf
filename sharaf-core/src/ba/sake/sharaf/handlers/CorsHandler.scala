package ba.sake.sharaf.handlers

import sttp.model.HeaderNames
import ba.sake.sharaf.*

// https://www.moesif.com/blog/technical/cors/Authoritative-Guide-to-CORS-Cross-Origin-Resource-Sharing-for-REST-APIs/
final class CorsHandler(corsSettings: CorsSettings, next: SharafHandler) extends SharafHandler {
  private val originHeader = HttpString(HeaderNames.Origin)
  private val accessControlRequestMethodHeader = HttpString(HeaderNames.AccessControlRequestMethod)
  private val varyHeader = HttpString(HeaderNames.Vary)
  private val accessControlAllowCredentialsHeader = HttpString(HeaderNames.AccessControlAllowCredentials)
  private val accessControlAllowOriginHeader = HttpString(HeaderNames.AccessControlAllowOrigin)
  private val accessControlAllowMethodsHeader = HttpString(HeaderNames.AccessControlAllowMethods)
  private val accessControlAllowHeadersHeader = HttpString(HeaderNames.AccessControlAllowHeaders)
  private val accessControlMaxAgeHeader = HttpString(HeaderNames.AccessControlMaxAge)
  private val appliesToEveryPath = corsSettings.pathPrefixes.contains("/")
  private val allowsAnyOrigin = corsSettings.allowedOrigins.contains("*")
  private val canReflectOrigin = !allowsAnyOrigin && corsSettings.allowedOrigins.nonEmpty
  private val allowedMethodsValue = corsSettings.allowedHttpMethods.map(_.toString).mkString(", ")
  private val allowedHeadersValue = corsSettings.allowedHttpHeaders.map(_.toString).mkString(", ")
  private val preflightMaxAgeValue = corsSettings.preflightMaxAge.getSeconds.toString
  private val allowCredentialsValue = corsSettings.allowCredentials.toString

  override def handle(context: RequestContext): Response[?] = {
    val applies = appliesToEveryPath || {
      val path = "/" + context.params.path.segments.mkString("/")
      corsSettings.pathPrefixes.exists(path.startsWith)
    }
    if !applies then next.handle(context)
    else {
      val isOptions = context.params.method == HttpMethod.OPTIONS
      val origin =
        if isOptions || canReflectOrigin then context.request.headerValues(originHeader)
        else None
      val preflight = isOptions &&
        origin.exists(_.nonEmpty) &&
        context.request.headerValues(accessControlRequestMethodHeader).exists(_.nonEmpty)
      if preflight then {
        setCorsHeaders(Response.default, origin)
          .settingHeader(accessControlAllowMethodsHeader, allowedMethodsValue)
          .settingHeader(accessControlAllowHeadersHeader, allowedHeadersValue)
          .settingHeader(accessControlMaxAgeHeader, preflightMaxAgeValue)
      } else setCorsHeaders(next.handle(context), origin)
    }
  }

  private def setCorsHeaders[T](res: Response[T], origin: Option[Seq[String]]): Response[T] = {
    val allowOrigin =
      if allowsAnyOrigin then Some("*")
      else origin.flatMap(_.headOption).filter(value => corsSettings.allowedOrigins.contains(value))
    val currentVary = if allowsAnyOrigin then Seq.empty else {
      val bodyHeaders = res.body.toSeq.flatMap(res.rw.headers)
      res.headerUpdates.effective(varyHeader, bodyHeaders).getOrElse(Seq.empty)
    }
    val needsVary = !allowsAnyOrigin && !variesOnOrigin(currentVary)
    val withVary = if needsVary then res.settingHeader(varyHeader, currentVary :+ "Origin") else res
    val withCredentials = withVary.settingHeader(accessControlAllowCredentialsHeader, allowCredentialsValue)
    allowOrigin match {
      case Some(value) => withCredentials.settingHeader(accessControlAllowOriginHeader, value)
      case None        => withCredentials
    }
  }

  private def variesOnOrigin(values: Seq[String]): Boolean =
    values.exists { value =>
      value
        .split(",")
        .exists(token => {
          val normalized = token.trim
          normalized == "*" || normalized.equalsIgnoreCase("Origin")
        })
    }
}
