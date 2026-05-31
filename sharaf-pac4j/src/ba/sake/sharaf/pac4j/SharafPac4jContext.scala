package ba.sake.sharaf.pac4j

import java.io.{ByteArrayOutputStream, ObjectOutputStream, ByteArrayInputStream, ObjectInputStream}
import java.util.{Optional as JOptional, Base64}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import org.pac4j.core.context.{Cookie as Pac4jCookie, FrameworkParameters, WebContext, WebContextFactory}
import org.pac4j.core.context.session.{SessionStore, SessionStoreFactory}
import org.pac4j.core.exception.http.{HttpAction, WithLocationAction, WithContentAction}
import org.pac4j.core.http.adapter.HttpActionAdapter
import ba.sake.sharaf.*

final class SharafPac4jContext(
    private val request: Request,
    private val fullUrl: String,
    private val methodName: String,
) extends WebContext
    with SessionStore
    with HttpActionAdapter {

  var successResponse: Option[Response[?]] = None

  private val cachedUri: java.net.URI = java.net.URI(fullUrl)

  private var mutableResponseHeaders: Map[String, String] = Map.empty
  private var mutableResponseCookies: Seq[Pac4jCookie] = Seq.empty
  private var mutableRequestAttributes: Map[String, Any] = Map.empty
  private var mutableActionResponse: Option[Response[String]] = None

  // ── WebContext reads ────────────────────────────────

  override def getRequestParameter(name: String): JOptional[String] =
    request.queryParamsRaw.get(name).flatMap(_.headOption).toJava

  override def getRequestParameters(): java.util.Map[String, Array[String]] =
    request.queryParamsRaw.view.mapValues(_.toArray).toMap.asJava

  override def getRequestAttribute(name: String): JOptional[AnyRef] =
    mutableRequestAttributes.get(name).map(_.asInstanceOf[AnyRef]).toJava

  override def setRequestAttribute(name: String, value: Any): Unit =
    mutableRequestAttributes += (name -> value)

  override def getRequestHeader(name: String): JOptional[String] =
    request.headers.get(HttpString(name)).flatMap(_.headOption).toJava

  override def getRequestMethod(): String = methodName

  override def getRemoteAddr(): String = "127.0.0.1"

  override def getServerName(): String = Option(cachedUri.getHost).getOrElse("localhost")

  override def getServerPort(): Int = cachedUri.getPort match { case -1 => 80; case p => p }

  override def getScheme(): String = Option(cachedUri.getScheme).getOrElse("http")

  override def isSecure(): Boolean = getScheme == "https"

  override def getFullRequestURL(): String = fullUrl

  override def getRequestCookies(): java.util.Collection[Pac4jCookie] =
    request.cookies.map { c =>
      val pc = new Pac4jCookie(c.name, c.value)
      c.path.foreach(pc.setPath)
      c.domain.foreach(pc.setDomain)
      pc.setSecure(c.secure)
      pc.setHttpOnly(c.httpOnly)
      pc.setMaxAge(c.maxAge.getOrElse(-1))
      c.sameSiteMode.foreach(pc.setSameSitePolicy)
      pc
    }.asJavaCollection

  override def getPath(): String = cachedUri.getPath

  override def getRequestContent(): String =
    try request.bodyString
    catch case _: Exception => ""

  override def getCharacterEncoding(): JOptional[String] = JOptional.of("UTF-8")

  // ── WebContext writes ───────────────────────────────

  override def setResponseHeader(name: String, value: String): Unit =
    mutableResponseHeaders += (name -> value)

  override def getResponseHeader(name: String): JOptional[String] =
    mutableResponseHeaders.get(name).toJava

  override def setResponseContentType(content: String): Unit =
    mutableResponseHeaders += ("Content-Type" -> content)

  override def addResponseCookie(cookie: Pac4jCookie): Unit =
    mutableResponseCookies = mutableResponseCookies :+ cookie

  // ── HttpActionAdapter ───────────────────────────────

  override def adapt(action: HttpAction, context: WebContext): AnyRef = {
    val status = action.getCode
    val res: Response[String] = action match {
      case a: WithLocationAction =>
        Response.withStatus(sttp.model.StatusCode.unsafeApply(status))
          .settingHeader("Location", a.getLocation)
      case a: WithContentAction =>
        val body = Option(a.getContent).filter(_.nonEmpty)
        body.map(b => Response.withStatus(sttp.model.StatusCode.unsafeApply(status)).withBody(b))
          .getOrElse(Response.withStatus(sttp.model.StatusCode.unsafeApply(status)))
      case _ =>
        Response.withStatus(sttp.model.StatusCode.unsafeApply(status))
    }
    val withHeaders = mutableResponseHeaders.foldLeft(res) { case (r, (k, v)) =>
      r.settingHeader(k, v)
    }
    val withCookies = mutableResponseCookies.foldLeft(withHeaders) { case (r, pc) =>
      r.settingCookie(convertPac4jCookie(pc))
    }
    mutableActionResponse = Some(withCookies)
    null
  }

  // ── SessionStore ────────────────────────────────────

  override def getSessionId(context: WebContext, createSession: Boolean): JOptional[String] =
    try JOptional.of(Session.current.id)
    catch case _: Exception => JOptional.empty()

  override def get(context: WebContext, key: String): JOptional[AnyRef] =
    try Session.current.getOpt[String](key).flatMap { str =>
      try {
        val bytes = Base64.getDecoder.decode(str)
        val ois = ObjectInputStream(ByteArrayInputStream(bytes))
        try Option(ois.readObject())
        finally ois.close()
      } catch case _: Exception => None
    }.toJava.asInstanceOf[JOptional[AnyRef]]
    catch case _: Exception => JOptional.empty()

  override def set(context: WebContext, key: String, value: Any): Unit =
    try {
      val baos = ByteArrayOutputStream()
      val oos = ObjectOutputStream(baos)
      try {
        oos.writeObject(value)
        oos.flush()
        Session.current.set[String](key, Base64.getEncoder.encodeToString(baos.toByteArray))
      } finally {
        oos.close()
        baos.close()
      }
    } catch case _: Exception => ()

  override def destroySession(context: WebContext): Boolean =
    try { Session.current.invalidate(); true }
    catch case _: Exception => false

  override def getTrackableSession(context: WebContext): JOptional[AnyRef] = JOptional.empty()

  override def buildFromTrackableSession(context: WebContext, trackableSession: Any): JOptional[SessionStore] =
    JOptional.empty()

  override def renewSession(context: WebContext): Boolean =
    try { Session.current.regenerate(); true }
    catch case _: Exception => false

  // ── Response finalization ───────────────────────────

  def toResponse(): Response[?] =
    mutableActionResponse.orElse(successResponse).getOrElse(
      Response.withStatus(sttp.model.StatusCode.InternalServerError)
    )

  private def convertPac4jCookie(pc: Pac4jCookie): Cookie =
    Cookie(
      name = pc.getName,
      value = pc.getValue,
      path = Option(pc.getPath),
      domain = Option(pc.getDomain),
      maxAge = Option(pc.getMaxAge).filter(_ >= 0),
      secure = pc.isSecure,
      httpOnly = pc.isHttpOnly,
      sameSite = pc.getSameSitePolicy != null,
      sameSiteMode = Option(pc.getSameSitePolicy),
    )
}

object SharafPac4jContext {

  private val currentContext = new ThreadLocal[SharafPac4jContext]()

  def webContextFactory: WebContextFactory =
    (params: FrameworkParameters) => params match {
      case sfp: SharafFrameworkParameters =>
        SharafPac4jContext(sfp.request, sfp.fullUrl, sfp.method.name)
      case _ => throw IllegalArgumentException("Expected SharafFrameworkParameters")
    }

  def sessionStoreFactory: SessionStoreFactory =
    (params: FrameworkParameters) => params match {
      case sfp: SharafFrameworkParameters =>
        SharafPac4jContext(sfp.request, sfp.fullUrl, sfp.method.name)
      case _ => throw IllegalArgumentException("Expected SharafFrameworkParameters")
    }

  /** Static singleton adapter — delegates to the current-request context via ThreadLocal.
    * Safe to share across requests since each request sets the correct context before use.
    */
  val globalHttpActionAdapter: HttpActionAdapter = (action: HttpAction, _: WebContext) =>
    Option(currentContext.get()) match {
      case Some(sc) => sc.adapt(action, null)
      case None => null
    }

  def withCurrentContext[T](ctx: SharafPac4jContext)(f: => T): T = {
    currentContext.set(ctx)
    try f
    finally currentContext.remove()
  }
}

final class SharafFrameworkParameters(
    val request: Request,
    val fullUrl: String,
    val method: HttpMethod,
) extends FrameworkParameters
