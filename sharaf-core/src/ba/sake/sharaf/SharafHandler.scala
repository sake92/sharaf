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
      next: SharafHandler = DefaultNotFoundHandler
  ): SharafHandler =
    FilesHandler(directoryPath, next)

  def exceptions(wrappedHandler: SharafHandler): SharafHandler =
    exceptions(ExceptionMapper.default, wrappedHandler)
  def exceptions(exceptionMapper: ExceptionMapper, wrappedHandler: SharafHandler): SharafHandler =
    ExceptionHandler(exceptionMapper, wrappedHandler)

  def cors(next: SharafHandler): SharafHandler =
    cors(CorsSettings.default, next)
  def cors(corsSettings: CorsSettings, next: SharafHandler): SharafHandler =
    CorsHandler(corsSettings, next)

case class RequestContext(
    params: RequestParams,
    request: Request
)
