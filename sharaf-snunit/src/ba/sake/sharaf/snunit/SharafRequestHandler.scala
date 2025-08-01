package ba.sake.sharaf.snunit

import java.io.ByteArrayOutputStream
import snunit.{Request as SnunitRequest, *}
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*

class SharafRequestHandler(sharafHandler: SharafHandler) extends RequestHandler {
  override def handleRequest(snunitRequest: SnunitRequest): Unit = {
    val reqParams = fillReqParams(snunitRequest)
    val req = SnunitSharafRequest.create(snunitRequest)
    val requestContext = RequestContext(reqParams, req)
    val res = sharafHandler.handle(requestContext)
    val headers = buildHeaders(res.headerUpdates)
    res.body match {
      case Some(body) =>
        val aos = new ByteArrayOutputStream
        res.rw.write(body, aos)
        send(snunitRequest)(StatusCode(res.status.code), aos.toByteArray(), headers)
      case None =>
        send(snunitRequest)(StatusCode(res.status.code), "", headers)
    }
  }

  private def buildHeaders(hu: HeaderUpdates): Headers = {
    val headerValues = hu.updates.flatMap {
      case HeaderUpdate.Set(name, values) =>
        Seq(name.value -> values.head)
      case _ => Seq.empty
    }
    Headers(headerValues*)
  }

  private def fillReqParams(req: SnunitRequest): RequestParams = {
    val method = HttpMethod.valueOf(req.method)
    val originalPath = req.path
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
