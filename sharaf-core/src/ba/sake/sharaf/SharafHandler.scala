package ba.sake.sharaf

import sttp.model.StatusCode
import ba.sake.sharaf.routing.{RequestParams, Routes}
import ba.sake.sharaf.handlers.*
import ba.sake.sharaf.session.{SessionHandler, SessionStore, SessionConfig, InMemorySessionStore}

trait SharafHandler:
  def handle(context: RequestContext): Response[?]

object SharafHandler:

  val DefaultNotFoundHandler: SharafHandler =
    _ => Response.withStatus(StatusCode.NotFound).withBody("Not Found")

  def routes(
      routess: Routes,
      notFoundHandler: SharafHandler = DefaultNotFoundHandler
  ): SharafHandler =
    RoutesHandler(routess, notFoundHandler)

  def files(
      directoryPath: java.nio.file.Path,
      notFoundHandler: SharafHandler = DefaultNotFoundHandler
  ): SharafHandler =
    FilesHandler(directoryPath, notFoundHandler)

  def classpathResources(
      rootPath: String,
      notFoundHandler: SharafHandler = DefaultNotFoundHandler
  ): SharafHandler =
    ClasspathResourcesHandler(rootPath, notFoundHandler)

  def exceptions(
      wrappedHandler: SharafHandler,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default
  ): SharafHandler =
    ExceptionHandler(exceptionMapper, wrappedHandler)

  def cors(next: SharafHandler, corsSettings: CorsSettings = CorsSettings.default): SharafHandler =
    if corsSettings eq CorsSettings.disabled then next
    else CorsHandler(corsSettings, next)

  /** Compresses eligible response bodies with gzip when the request accepts it.
    *
    * Compression is streamed to the response body. For that reason the compressed response length is not known in
    * advance and HTTP/1.1 adapters may use chunked transfer encoding.
    */
  def gzip(next: SharafHandler, settings: GzipSettings = GzipSettings.default): SharafHandler =
    GzipHandler(settings, next)

  def sessions(
      next: SharafHandler,
      store: Option[SessionStore] = None,
      config: SessionConfig = SessionConfig.default
  ): SharafHandler =
    SessionHandler(store.getOrElse(InMemorySessionStore(config)), config, next)

case class RequestContext(
    params: RequestParams,
    request: Request
)
