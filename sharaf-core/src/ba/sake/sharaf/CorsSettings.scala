package ba.sake.sharaf

import java.time.Duration

// stolen from Play
// https://www.playframework.com/documentation/2.8.x/CorsFilter#Configuring-the-CORS-filter
// https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_request_header
final class CorsSettings private (
    val pathPrefixes: Set[String],
    val allowedOrigins: Set[String],
    val allowedHttpMethods: Set[HttpMethod],
    val allowedHttpHeaders: Set[HttpString],
    val allowCredentials: Boolean,
    val preflightMaxAge: Duration
) {

  def withPathPrefixes(pathPrefixes: Set[String]): CorsSettings =
    copy(pathPrefixes = pathPrefixes)

  def withAllowedOrigins(allowedOrigins: Set[String]): CorsSettings =
    copy(allowedOrigins = allowedOrigins)

  def withAllowedHttpMethods(allowedHttpMethods: Set[HttpMethod]): CorsSettings =
    copy(allowedHttpMethods = allowedHttpMethods)

  def withAllowedHttpHeaders(allowedHttpHeaders: Set[HttpString]): CorsSettings =
    copy(allowedHttpHeaders = allowedHttpHeaders)

  def withAllowCredentials(allowCredentials: Boolean): CorsSettings =
    copy(allowCredentials = allowCredentials)

  def withPreflightMaxAge(preflightMaxAge: Duration): CorsSettings =
    copy(preflightMaxAge = preflightMaxAge)

  private def copy(
      pathPrefixes: Set[String] = pathPrefixes,
      allowedOrigins: Set[String] = allowedOrigins,
      allowedHttpMethods: Set[HttpMethod] = allowedHttpMethods,
      allowedHttpHeaders: Set[HttpString] = allowedHttpHeaders,
      allowCredentials: Boolean = allowCredentials,
      preflightMaxAge: Duration = preflightMaxAge
  ) = new CorsSettings(
    pathPrefixes,
    allowedOrigins,
    allowedHttpMethods,
    allowedHttpHeaders,
    allowCredentials,
    preflightMaxAge
  )

  override def toString: String = s"""
    CorsSettings(
      pathPrefixes = $pathPrefixes,
      allowedOrigins = $allowedOrigins,
      allowedHttpMethods = $allowedHttpMethods,
      allowedHttpHeaders = $allowedHttpHeaders,
      allowCredentials = $allowCredentials,
      preflightMaxAge = $preflightMaxAge
    )
  """
}

object CorsSettings:
  val default: CorsSettings = new CorsSettings(
    pathPrefixes = Set("/"),
    allowedOrigins = Set.empty,
    allowedHttpMethods = Set(
      HttpMethod.GET,
      HttpMethod.HEAD,
      HttpMethod.OPTIONS,
      HttpMethod.POST,
      HttpMethod.PUT,
      HttpMethod.PATCH,
      HttpMethod.DELETE
    ),
    allowedHttpHeaders = Set(Headers.ACCEPT, Headers.ACCEPT_LANGUAGE, Headers.CONTENT_LANGUAGE, Headers.CONTENT_TYPE),
    allowCredentials = false,
    preflightMaxAge = Duration.ofDays(3)
  )
