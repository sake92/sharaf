package ba.sake.sharaf.potateh

import ba.sake.potateh.{Headers, ResponseBody, Server, ServerConfig}
import ba.sake.sharaf.*

object PotatehSharafServer {

  /** Starts a server owned by the caller. Call stop() on the returned server at shutdown. */
  def start(config: ServerConfig, handler: SharafHandler): Server =
    Server.start(config) { underlying =>
      val request = PotatehSharafRequest(underlying)
      val segments = Option(request.uri.getPath).getOrElse("").stripPrefix("/").split("/")
      val path = if segments.sameElements(Array("")) then Path() else Path(segments*)
      val context = RequestContext((HttpMethod.valueOf(underlying.method), path), request)
      toResponse(handler.handle(context))
    }

  def start(
      config: ServerConfig,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): Server = {
    val handler = SharafHandler.exceptions(
      SharafHandler.cors(SharafHandler.routes(routes, notFoundHandler), corsSettings),
      exceptionMapper
    )
    start(config, handler)
  }

  private def toResponse[T](response: Response[T]): ba.sake.potateh.Response = {
    val contentHeaders = response.body.toSeq.flatMap(response.rw.headers)
    val initial = contentHeaders.foldLeft(Headers.empty) { case (headers, (name, values)) =>
      setHeader(headers, name, values)
    }
    val updated = response.headerUpdates.updates.foldLeft(initial) {
      case (headers, HeaderUpdate.Set(name, values)) => setHeader(headers, name, values)
      case (headers, HeaderUpdate.Remove(name))      => headers.remove(name.toString)
    }
    val headers = response.cookieUpdates.updates.foldLeft(updated) { (headers, cookie) =>
      headers.add("Set-Cookie", renderCookie(cookie))
    }
    val body = response.body match {
      case None        => ResponseBody.Empty
      case Some(value) => ResponseBody.Writer() { output => response.rw.write(value, output) }
    }
    ba.sake.potateh.Response(response.status.code, headers, body)
  }

  private def setHeader(headers: Headers, name: HttpString, values: Seq[String]): Headers =
    values.foldLeft(headers.remove(name.toString)) { (result, value) => result.add(name.toString, value) }

  private def renderCookie(cookie: Cookie): String = {
    import java.time.ZoneOffset
    import java.time.format.DateTimeFormatter
    val result = new StringBuilder(s"${cookie.name}=${cookie.value}")
    cookie.path.foreach(value => result.append(s"; Path=$value"))
    cookie.domain.foreach(value => result.append(s"; Domain=$value"))
    cookie.maxAge.foreach(value => result.append(s"; Max-Age=$value"))
    cookie.expires.foreach { value =>
      val expires = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(value)
      result.append(s"; Expires=$expires")
    }
    if cookie.secure then result.append("; Secure")
    if cookie.httpOnly then result.append("; HttpOnly")
    cookie.sameSiteMode.foreach(value => result.append(s"; SameSite=$value"))
    result.toString
  }
}
