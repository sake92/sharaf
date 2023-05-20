package ba.sake.sharaf

import io.undertow.util.Headers
import ba.sake.tupson.*

case class Response(
    body: String,
    status: Int = 200,
    headers: Map[String, Seq[String]] = Map(Headers.CONTENT_TYPE_STRING -> Seq("text/plain"))
) {
  def withStatus(status: Int) = copy(status = status)

  def withHeader(name: String, values: Seq[String]) =
    copy(headers = headers + (name -> values))
  def withHeader(name: String, value: String) =
    copy(headers = headers + (name -> Seq(value)))
}

object Response {

  def withHtmlBody(body: String): Response =
    Response(body.toJson)
      .withHeader(Headers.CONTENT_TYPE_STRING, "text/html; charset=utf-8")

  def withJsonBody[T: JsonRW](body: T): Response =
    Response(body.toJson)
      .withHeader(Headers.CONTENT_TYPE_STRING, "application/json")

  def withJsonBody[T: JsonRW](bodyOpt: Option[T], name: String): Response = bodyOpt match
    case None => throw new NotFoundException(name)
    case Some(body) =>
      Response(body.toJson)
        .withHeader(Headers.CONTENT_TYPE_STRING, "application/json")
}
