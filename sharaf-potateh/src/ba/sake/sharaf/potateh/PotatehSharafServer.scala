package ba.sake.sharaf.potateh

import ba.sake.potateh.{Headers, ResponseBody, Server, ServerConfig}
import java.util.concurrent.Executor
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.{BadRequestException, UnsupportedMethodException}

object PotatehSharafServer {

  /** Starts a server owned by the caller. Call stop() on the returned server at shutdown. Failures before a request
    * context exists use the default exception mapper.
    */
  def start(config: ServerConfig, handler: SharafHandler): Server =
    startServer(config, handler, ExceptionMapper.default, None)

  /** The caller retains ownership of the executor, including after server.stop(). Each task serves a whole persistent
    * connection. Small fixed pools can starve clients; rejected submissions must throw.
    */
  def start(config: ServerConfig, handler: SharafHandler, executor: Executor): Server =
    startServer(config, handler, ExceptionMapper.default, Some(executor))

  def start(
      config: ServerConfig,
      routes: Routes,
      corsSettings: CorsSettings = CorsSettings.default,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): Server = startRoutes(config, routes, corsSettings, exceptionMapper, notFoundHandler, None)

  /** Starts routes with a caller-owned executor; ownership and scheduling follow the handler overload. */
  def start(config: ServerConfig, routes: Routes, executor: Executor): Server =
    startRoutes(
      config,
      routes,
      CorsSettings.default,
      ExceptionMapper.default,
      SharafHandler.DefaultNotFoundHandler,
      Some(executor)
    )

  def start(
      config: ServerConfig,
      routes: Routes,
      corsSettings: CorsSettings,
      exceptionMapper: ExceptionMapper,
      notFoundHandler: SharafHandler,
      executor: Executor
  ): Server = startRoutes(config, routes, corsSettings, exceptionMapper, notFoundHandler, Some(executor))

  private def startRoutes(
      config: ServerConfig,
      routes: Routes,
      corsSettings: CorsSettings,
      exceptionMapper: ExceptionMapper,
      notFoundHandler: SharafHandler,
      executor: Option[Executor]
  ): Server = {
    val handler = SharafHandler.exceptions(
      SharafHandler.cors(SharafHandler.routes(routes, notFoundHandler), corsSettings),
      exceptionMapper
    )
    startServer(config, handler, exceptionMapper, executor)
  }

  private def startServer(
      config: ServerConfig,
      handler: SharafHandler,
      bootstrapMapper: ExceptionMapper,
      executor: Option[Executor]
  ): Server = {
    val handle: ba.sake.potateh.Request => ba.sake.potateh.Response = underlying => {
      val request = PotatehSharafRequest(underlying)
      try {
        val context =
          try {
            val method = HttpMethod.values
              .find(_.name == underlying.method)
              .getOrElse(throw UnsupportedMethodException(underlying.method))
            Right(RequestContext((method, request.path), request))
          } catch {
            case error: BadRequestException        => Left(error)
            case error: UnsupportedMethodException => Left(error)
          }
        val response = context match {
          case Right(value) => handler.handle(value)
          case Left(error)  => bootstrapMapper.applyOrElse(error, (unmapped: Throwable) => throw unmapped)
        }
        toResponse(response, underlying.method)
      } finally request.closeUploads()
    }
    executor match {
      case Some(value) => Server.start(config, value)(handle)
      case None        => Server.start(config)(handle)
    }
  }

  private def toResponse[T](response: Response[T], requestMethod: String): ba.sake.potateh.Response = {
    val contentHeaders = response.body.toSeq.flatMap(response.rw.headers)
    val updated = response.headerUpdates.resolve(contentHeaders).foldLeft(Headers.empty) {
      case (headers, HeaderUpdate.Set(name, values)) => setHeader(headers, name, values)
      case (headers, HeaderUpdate.Remove(name))      => headers.remove(name.toString)
    }
    val withCookies = response.cookieUpdates.updates.foldLeft(updated) { (headers, cookie) =>
      headers.add("Set-Cookie", CookieHeaders.render(cookie))
    }
    val connectionFields = withCookies.all("Connection").flatMap(_.split(',')).map(_.trim)
    require(!connectionFields.exists(_.equalsIgnoreCase("close")), "Potateh does not support response-requested close")
    require(withCookies.all("Upgrade").isEmpty, "Potateh does not support protocol upgrades")
    require(withCookies.all("Trailer").isEmpty, "Potateh does not support response trailers")
    val lengths = withCookies.all("Content-Length")
    val length = lengths.headOption.map { text =>
      require(
        lengths.size == 1 && text.nonEmpty && text.forall(c => c >= '0' && c <= '9'),
        "Invalid response Content-Length"
      )
      text.toLongOption.getOrElse(throw new IllegalArgumentException("Response Content-Length overflow"))
    }
    require(length.isEmpty || withCookies.all("Transfer-Encoding").isEmpty, "Conflicting response framing")
    val managed = Seq("Content-Length", "Transfer-Encoding", "Connection", "Keep-Alive", "Upgrade", "Trailer")
    val headers = (managed ++ connectionFields).foldLeft(withCookies)((result, name) => result.remove(name))
    val body = response.body match {
      case None if length.nonEmpty && (requestMethod == "HEAD" || response.status.code == 304) =>
        ResponseBody.Writer(length)(_ => ())
      case None =>
        require(length.forall(_ == 0), "Nonzero Content-Length without a body")
        ResponseBody.Empty
      case Some(value) => ResponseBody.Writer(length) { output => response.rw.write(value, output) }
    }
    ba.sake.potateh.Response(response.status.code, headers, body)
  }

  private def setHeader(headers: Headers, name: HttpString, values: Seq[String]): Headers =
    values.foldLeft(headers.remove(name.toString)) { (result, value) => result.add(name.toString, value) }

}
