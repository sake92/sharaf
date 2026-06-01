package ba.sake.sharaf.pac4j

import org.pac4j.core.context.FrameworkParameters
import ba.sake.sharaf.{Request, HttpMethod}

/** pac4j [[FrameworkParameters]] carrying a Sharaf [[Request]] with its full URL and HTTP method.
  *
  * The [[Request]] trait does not carry the full request URL or HTTP method — those are
  * supplied by the server integration and passed through here.
  */
final class SharafFrameworkParameters(
    val request: Request,
    val fullUrl: String,
    val method: HttpMethod
) extends FrameworkParameters
