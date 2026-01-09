package ba.sake.sharaf

import sttp.model.StatusCode
import ba.sake.sharaf.routing.{RequestParams, Routes}
import ba.sake.sharaf.handlers.*

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
    CorsHandler(corsSettings, next)

case class RequestContext(
    params: RequestParams,
    request: Request
)
