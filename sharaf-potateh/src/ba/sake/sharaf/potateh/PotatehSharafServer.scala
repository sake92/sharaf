package ba.sake.sharaf.potateh

import ba.sake.potateh.{Headers, ResponseBody, Server, ServerConfig}
import java.util.concurrent.Executor
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.{BadRequestException, UnsupportedMethodException}

object PotatehSharafServer {
  private val ManagedHeaders =
    Vector("Content-Length", "Transfer-Encoding", "Connection", "Keep-Alive", "Upgrade", "Trailer")

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
      corsSettings: CorsSettings = CorsSettings.disabled,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): Server = startRoutes(config, routes, corsSettings, exceptionMapper, notFoundHandler, None)

  /** Starts routes with a caller-owned executor; ownership and scheduling follow the handler overload. */
  def start(config: ServerConfig, routes: Routes, executor: Executor): Server =
    startRoutes(
      config,
      routes,
      CorsSettings.disabled,
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
      underlying.onResponseComplete(request.closeUploads())
      val context =
        try {
          val method = parseMethod(underlying.method)
          Right(RequestContext((method = method, path = request.path), request))
        } catch {
          case error: BadRequestException        => Left(error)
          case error: UnsupportedMethodException => Left(error)
        }
      val response = context match {
        case Right(value) => handler.handle(value)
        case Left(error)  => bootstrapMapper.applyOrElse(error, (unmapped: Throwable) => throw unmapped)
      }
      toResponse(response, underlying.method)
    }
    executor match {
      case Some(value) => Server.start(config, value)(handle)
      case None        => Server.start(config)(handle)
    }
  }

  private def parseMethod(value: String): HttpMethod = value match {
    case "GET"     => HttpMethod.GET
    case "POST"    => HttpMethod.POST
    case "PUT"     => HttpMethod.PUT
    case "DELETE"  => HttpMethod.DELETE
    case "OPTIONS" => HttpMethod.OPTIONS
    case "PATCH"   => HttpMethod.PATCH
    case "HEAD"    => HttpMethod.HEAD
    case _         => throw UnsupportedMethodException(value)
  }

  private def toResponse[T](response: Response[T], requestMethod: String): ba.sake.potateh.Response = {
    val contentHeaders = response.body.toSeq.flatMap(response.rw.headers)
    val resolved = response.headerUpdates.resolve(contentHeaders)
    def values(name: String): Seq[String] = resolved.collect {
      case HeaderUpdate.Set(headerName, headerValues) if headerName.toString.equalsIgnoreCase(name) =>
        headerValues
    }.flatten

    val connectionFields = values("Connection").flatMap(_.split(',')).map(_.trim)
    require(!connectionFields.exists(_.equalsIgnoreCase("close")), "Potateh does not support response-requested close")
    require(values("Upgrade").isEmpty, "Potateh does not support protocol upgrades")
    require(values("Trailer").isEmpty, "Potateh does not support response trailers")
    val lengths = values("Content-Length")
    val length = lengths.headOption.map { text =>
      require(
        lengths.size == 1 && text.nonEmpty && text.forall(c => c >= '0' && c <= '9'),
        "Invalid response Content-Length"
      )
      text.toLongOption.getOrElse(throw new IllegalArgumentException("Response Content-Length overflow"))
    }
    val transferEncoding = values("Transfer-Encoding")
    require(length.isEmpty || transferEncoding.isEmpty, "Conflicting response framing")
    val bodyLength =
      if length.nonEmpty then length
      else if transferEncoding.nonEmpty then None
      else response.body.flatMap(KnownBodyLength.of)

    val fields = Vector.newBuilder[(name: String, value: String)]
    resolved.foreach {
      case HeaderUpdate.Set(name, headerValues) if !isManaged(name.toString, connectionFields) =>
        val headerName = name.toString
        headerValues.foreach(value => fields += ((name = headerName, value = value)))
      case _ => ()
    }
    response.cookieUpdates.updates.foreach { cookie =>
      fields += ((name = "Set-Cookie", value = CookieHeaders.render(cookie)))
    }
    val headers = Headers.from(fields.result())
    val body = response.body match {
      case None if length.nonEmpty && (requestMethod == "HEAD" || response.status.code == 304) =>
        ResponseBody.Writer(length)(_ => ())
      case None =>
        require(length.forall(_ == 0), "Nonzero Content-Length without a body")
        ResponseBody.Empty
      case Some(value) => ResponseBody.Writer(bodyLength) { output => response.rw.write(value, output) }
    }
    ba.sake.potateh.Response(response.status.code, headers, body)
  }

  private def isManaged(name: String, connectionFields: Seq[String]): Boolean =
    ManagedHeaders.exists(name.equalsIgnoreCase) ||
      connectionFields.exists(name.equalsIgnoreCase)

}
