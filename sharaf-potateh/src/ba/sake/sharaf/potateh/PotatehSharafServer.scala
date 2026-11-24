package ba.sake.sharaf.potateh

import java.net.InetSocketAddress
import ba.sake.potateh.{Server, ServerConfig}
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.{BadRequestException, UnsupportedMethodException}

final class PotatehSharafServer private (config: ServerConfig, startServer: ServerConfig => Server) {
  private var server: Option[Server] = None
  private var stopped = false

  def start(): Unit = synchronized {
    require(server.isEmpty && !stopped, "Server has already been started or stopped")
    server = Some(startServer(config))
  }

  def port: Int = synchronized {
    server.getOrElse(throw new IllegalStateException("Server has not been started")).address.getPort
  }

  def stop(): Unit = {
    val running = synchronized {
      stopped = true
      server
    }
    running.foreach(_.stop())
  }

  def withConfig(value: ServerConfig): PotatehSharafServer =
    new PotatehSharafServer(value, startServer)
}

object PotatehSharafServer {
  def apply(
      routes: Routes,
      host: String = "127.0.0.1",
      port: Int = 0,
      corsSettings: CorsSettings = CorsSettings.disabled,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default,
      notFoundHandler: SharafHandler = SharafHandler.DefaultNotFoundHandler
  ): PotatehSharafServer = {
    val config = ServerConfig.Default.withBindAddress(new InetSocketAddress(host, port))
    val handler = SharafHandler.exceptions(
      SharafHandler.cors(SharafHandler.routes(routes, notFoundHandler), corsSettings),
      exceptionMapper
    )
    new PotatehSharafServer(config, value => startServer(value, handler, exceptionMapper))
  }

  def apply(handler: SharafHandler, host: String, port: Int): PotatehSharafServer = {
    val config = ServerConfig.Default.withBindAddress(new InetSocketAddress(host, port))
    new PotatehSharafServer(config, value => startServer(value, handler, ExceptionMapper.default))
  }

  private def startServer(
      config: ServerConfig,
      handler: SharafHandler,
      bootstrapMapper: ExceptionMapper
  ): Server = {
    val handle: ba.sake.potateh.Request => ba.sake.potateh.Response = underlying => {
      val request = PotatehSharafRequest(underlying)
      underlying.onResponseComplete(request.closeUploads())
      val context =
        try {
          val method = PotatehConversions.parseMethod(underlying.method)
          val path = RequestDecoding.path(underlying.rawPath.getOrElse(""))
          Right(RequestContext((method = method, path = path), request))
        } catch {
          case error: BadRequestException        => Left(error)
          case error: UnsupportedMethodException => Left(error)
        }
      val response = context match {
        case Right(value) => handler.handle(value)
        case Left(error)  => bootstrapMapper.applyOrElse(error, (unmapped: Throwable) => throw unmapped)
      }
      PotatehConversions.toResponse(response, underlying.method)
    }
    Server.start(config)(handle)
  }

}
