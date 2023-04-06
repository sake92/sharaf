package ba.sake.sharaf

import io.undertow.util.Headers
import ba.sake.tupson.*

case class Response(
    body: String,
    status: Int = 200,
    headers: Map[String, Seq[String]] = Map.empty
) {
  def withStatus(status: Int) = copy(status = status)

  def withHeader(name: String, values: Seq[String]) =
    copy(headers = headers + (name -> values))
  def withHeader(name: String, value: String) =
    copy(headers = headers + (name -> Seq(value)))
}

object Response {
  def json[T: JsonRW](body: T): Response =
    Response(body.toJson)
      .withHeader(Headers.CONTENT_TYPE_STRING, "application/json")
}
