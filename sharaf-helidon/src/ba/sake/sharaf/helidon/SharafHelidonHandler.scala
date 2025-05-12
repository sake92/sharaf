package ba.sake.sharaf.helidon

import io.helidon.webserver.http.{Handler, ServerRequest, ServerResponse}
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*

class SharafHelidonHandler(routes: HelidonSharafRoutes) extends Handler {

  override def handle(helidonReq: ServerRequest, helidonRes: ServerResponse): Unit = {
    given HelidonSharafRequest = HelidonSharafRequest.create(helidonReq)
    val reqParams = fillReqParams(helidonReq)
    routes.definition.lift(reqParams) match {
      case Some(res) =>
        ResponseUtils.writeResponse(res, helidonRes)
      case None =>
        // will be catched by ExceptionHandler
        throw exceptions.NotFoundException("route")
    }
  }

  private def fillReqParams(req: ServerRequest): RequestParams = {
    val method = HttpMethod.valueOf(req.prologue().method().text())
    val originalPath = req.path().path()
    val relPath =
      if originalPath.startsWith("/") then originalPath.drop(1)
      else originalPath
    val pathSegments = relPath.split("/")
    val path =
      if pathSegments.size == 1 && pathSegments.head == ""
      then Path()
      else Path(pathSegments*)
    (method, path)
  }
}
