package ba.sake.sharaf

import ba.sake.sharaf.routing.{RequestParams, Routes}
import ba.sake.sharaf.handlers.*

trait SharafHandler:
  def handle(context: RequestContext): Response[?]

object SharafHandler:
  def routes(routess: Routes): SharafHandler =
    routes(routess, None)
  def routes(routess: Routes, notFoundHandler: Option[Request => Response[?]]): SharafHandler =
    RoutesHandler(routess, notFoundHandler)

  def exceptions(next: SharafHandler): SharafHandler =
    exceptions(ExceptionMapper.default, next)
  def exceptions(exceptionMapper: ExceptionMapper, next: SharafHandler): SharafHandler =
    ExceptionHandler(exceptionMapper, next)

  def cors(next: SharafHandler): SharafHandler =
    cors(CorsSettings.default, next)
  def cors(corsSettings: CorsSettings, next: SharafHandler): SharafHandler =
    CorsHandler(corsSettings, next)

case class RequestContext(
    params: RequestParams,
    request: Request
)
