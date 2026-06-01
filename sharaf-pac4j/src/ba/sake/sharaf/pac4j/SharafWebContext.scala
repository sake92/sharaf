package ba.sake.sharaf.pac4j

import java.util.{Collection as JCollection, Optional, HashMap as JHashMap}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import org.pac4j.core.context.{Cookie as Pac4jCookie, FrameworkParameters, WebContext, WebContextFactory}
import ba.sake.sharaf.{Cookie as SharafCookie, Request, Response, HttpString, HttpMethod}

/** Adapts a Sharaf [[Request]] to pac4j's [[WebContext]].
  *
  * Receives the full URL and HTTP method from the framework (since the [[Request]] trait
  * does not carry these). Accumulates response modifications (cookies, headers, content
  * type) in mutable collections, then merges them back into a Sharaf [[Response]] via
  * [[supplementResponse]].
  */
final class SharafWebContext(
    private val request: Request,
    val fullUrl: String,
    val method: HttpMethod
) extends WebContext:

  private val cachedUri = new java.net.URI(fullUrl)

  // --- Response accumulation ---
  private val _responseCookies = scala.collection.mutable.Buffer.empty[SharafCookie]
  private val _responseHeaders = new JHashMap[String, String]()
  private var _responseContentType: Option[String] = None

  // --- Request attributes (pac4j's internal state) ---
  private val _requestAttributes = new JHashMap[String, AnyRef]()

  /** Merge accumulated response modifications into a Sharaf [[Response]]. */
  def supplementResponse(res: Response[?]): Response[?] =
    val withCookies = _responseCookies.foldLeft(res)((r, c) => r.settingCookie(c))
    val withHeaders = _responseHeaders.asScala.foldLeft(withCookies) { case (r, (k, v)) =>
      r.settingHeader(k, Seq(v))
    }
    _responseContentType.fold(withHeaders)(ct => withHeaders.settingHeader("Content-Type", ct))

  /** Returns the underlying Sharaf [[Request]]. */
  def getNativeRequest: Request = request

  // --- WebContext: request methods ---

  override def getRequestURL(): String = fullUrl

  override def getRequestMethod(): String = method.name

  override def getRequestCookies(): JCollection[Pac4jCookie] =
    request.cookies.map { c =>
      val maxAge = c.maxAge match
        case None    => -1 // session cookie
        case Some(n) => n
      val pc = new Pac4jCookie(c.name, c.value)
      pc.setMaxAge(maxAge)
      c.path.foreach(pc.setPath)
      c.domain.foreach(pc.setDomain)
      pc.setSecure(c.secure)
      pc.setHttpOnly(c.httpOnly)
      c.sameSiteMode.foreach(pc.setSameSitePolicy)
      pc
    }.asJavaCollection

  def getRequestHeaders(): java.util.Map[String, String] =
    request.headers.map { (k, v) => k.value -> v.mkString(", ") }.asJava

  override def getRequestParameters(): java.util.Map[String, Array[String]] =
    request.queryParamsRaw.map { (k, v) => k -> v.toArray }.asJava

  override def getRequestParameter(name: String): Optional[String] =
    request.queryParamsRaw.get(name).flatMap(_.headOption).toJava

  override def getRequestHeader(name: String): Optional[String] =
    request.headers.collectFirst {
      case (k, vs) if k.value.equalsIgnoreCase(name) => vs.headOption
    }.flatten.toJava

  override def getRequestAttribute(name: String): Optional[AnyRef] =
    Optional.ofNullable(_requestAttributes.get(name))

  override def setRequestAttribute(name: String, value: AnyRef): Unit =
    _requestAttributes.put(name, value)

  override def getRemoteAddr(): String =
    request.headers.get(HttpString("X-Forwarded-For"))
      .flatMap(_.headOption)
      .getOrElse("127.0.0.1")

  override def getServerName(): String =
    request.headers.get(HttpString("Host"))
      .flatMap(_.headOption)
      .map(_.split(":").head)
      .getOrElse("localhost")

  override def getServerPort(): Int =
    request.headers.get(HttpString("Host"))
      .flatMap(_.headOption)
      .flatMap(_.split(":").lift(1))
      .flatMap(_.toIntOption)
      .getOrElse(if isSecure() then 443 else 80)

  override def getScheme(): String =
    request.headers.get(HttpString("X-Forwarded-Proto"))
      .flatMap(_.headOption)
      .getOrElse(if isSecure() then "https" else "http")

  override def isSecure(): Boolean =
    getScheme() == "https"

  override def getFullRequestURL(): String = fullUrl

  override def getPath(): String = cachedUri.getPath

  override def getRequestContent(): String = request.bodyString

  // --- WebContext: response methods ---

  override def addResponseCookie(cookie: Pac4jCookie): Unit =
    val maxAge = cookie.getMaxAge match
      case -1 => None           // session cookie
      case 0  => Some(0)        // delete cookie
      case n  => Some(n)
    val expires = cookie.getMaxAge match
      case 0 => Some(java.time.Instant.EPOCH)
      case _ => None
    val sc = SharafCookie(
      name = cookie.getName,
      value = cookie.getValue,
      path = Option(cookie.getPath),
      domain = Option(cookie.getDomain),
      maxAge = maxAge,
      expires = expires,
      secure = cookie.isSecure,
      httpOnly = cookie.isHttpOnly,
      sameSite = true,
      sameSiteMode = Option(cookie.getSameSitePolicy)
    )
    _responseCookies += sc

  override def setResponseHeader(name: String, value: String): Unit =
    _responseHeaders.put(name, value)

  override def getResponseHeader(name: String): Optional[String] =
    Optional.ofNullable(_responseHeaders.get(name))

  override def setResponseContentType(contentType: String): Unit =
    _responseContentType = Some(contentType)

  override def getProtocol(): String = "HTTP/1.1"

  override def getQueryString(): Optional[String] =
    val query = cachedUri.getQuery
    if query == null then Optional.empty() else Optional.of(query)

  override def getCharacterEncoding(): Optional[String] =
    request.headers.get(HttpString("Content-Type"))
      .flatMap(_.headOption)
      .flatMap(ct => ct.split("charset=").lift(1))
      .toJava

object SharafWebContext:
  /** pac4j [[WebContextFactory]] that creates [[SharafWebContext]] from [[SharafFrameworkParameters]]. */
  val factory: WebContextFactory = (params: FrameworkParameters) =>
    params match
      case sfp: SharafFrameworkParameters => new SharafWebContext(sfp.request, sfp.fullUrl, sfp.method)
      case _ => throw new IllegalArgumentException(
        s"Expected SharafFrameworkParameters, got ${params.getClass.getName}"
      )
