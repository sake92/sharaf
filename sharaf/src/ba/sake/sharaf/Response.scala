package ba.sake.sharaf

import io.undertow.util.StatusCodes
import io.undertow.util.HttpString

final class Response[T] private (
    val status: Int,
    val headers: Map[HttpString, Seq[String]],
    val body: Option[T]
)(using val rw: ResponseWritable[T]) {

  def withStatus(status: Int) =
    copy(status = status)

  def withHeader(name: HttpString, values: Seq[String]) =
    copy(headers = headers + (name -> values))
  def withHeader(name: HttpString, value: String) =
    copy(headers = headers + (name -> Seq(value)))

  def withBody[T2: ResponseWritable](body: T2): Response[T2] =
    copy(body = Some(body))

  private def copy[T2](
      status: Int = status,
      headers: Map[HttpString, Seq[String]] = headers,
      body: Option[T2] = body
  )(using ResponseWritable[T2]) = new Response(status, headers, body)
}

object Response {

  private val defaultRes = new Response[String](StatusCodes.OK, Map.empty, None)

  def apply[T: ResponseWritable] = defaultRes

  def withStatus(status: Int) =
    defaultRes.withStatus(status)

  def withHeader(name: HttpString, values: Seq[String]) =
    defaultRes.withHeader(name, values)

  def withHeader(name: HttpString, value: String) =
    defaultRes.withHeader(name, Seq(value))

  def withBody[T: ResponseWritable](body: T): Response[T] =
    defaultRes.withBody(body)

  def withBodyOpt[T: ResponseWritable](body: Option[T], name: String): Response[T] = body match
    case Some(value) => withBody(value)
    case None        => throw NotFoundException(name)

  def redirect(location: String): Response[String] =
    withStatus(StatusCodes.MOVED_PERMANENTLY).withHeader(HttpString("Location"), location)

}
