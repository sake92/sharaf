package ba.sake.sharaf.helidon

import io.helidon.webserver.http.{Handler, ServerRequest, ServerResponse}
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*

class SharafHelidonHandler(sharafHandler: SharafHandler) extends Handler {

  override def handle(helidonReq: ServerRequest, helidonRes: ServerResponse): Unit = {
    val reqParams = fillReqParams(helidonReq)
    val req = HelidonSharafRequest.create(helidonReq)
    val requestContext = RequestContext(reqParams, req)
    try {
      val res = sharafHandler.handle(requestContext)
      ResponseUtils.writeResponse(res, helidonRes)
    } finally req.closeUploads()
  }

  private def fillReqParams(req: ServerRequest): RequestParams = {
    val method = HttpMethod.valueOf(req.prologue().method().text())
    val path = RequestDecoding.path(req.path().rawPath())
    (method = method, path = path)
  }
}
