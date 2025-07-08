package ba.sake.sharaf.helidon

import io.helidon.webserver.http.{Handler, ServerRequest, ServerResponse}
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*

class SharafHelidonHandler(sharafHandler: SharafHandler) extends Handler {

  override def handle(helidonReq: ServerRequest, helidonRes: ServerResponse): Unit = {
    val reqParams = fillReqParams(helidonReq)
    val req = HelidonSharafRequest.create(helidonReq)
    val requestContext = RequestContext(reqParams, req)
    val res = sharafHandler.handle(requestContext) 
    ResponseUtils.writeResponse(res, helidonRes)
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
