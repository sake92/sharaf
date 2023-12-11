package ba.sake.sharaf

import io.undertow.util.StatusCodes

final class Response[T] private (
    val status: Int,
    val headers: Map[String, Seq[String]],
    val body: Option[T]
)(using val rw: ResponseWritable[T]) {

  def withStatus(status: Int) =
    copy(status = status)

  def withHeader(name: String, values: Seq[String]) =
    copy(headers = headers + (name -> values))
  def withHeader(name: String, value: String) =
    copy(headers = headers + (name -> Seq(value)))

  def withBody[T2: ResponseWritable](body: T2): Response[T2] =
    copy(body = Some(body))

  private def copy[T2](
      status: Int = status,
      headers: Map[String, Seq[String]] = headers,
      body: Option[T2] = body
  )(using ResponseWritable[T2]) = new Response(status, headers, body)
}

object Response {

  def apply[T: ResponseWritable] = new Response(StatusCodes.OK, Map.empty, None)

  def withStatus(status: Int) =
    Response[String].withStatus(status)

  def withHeader(name: String, values: Seq[String]) =
    Response[String].withHeader(name, values)

  def withHeader(name: String, value: String) =
    Response[String].withHeader(name, Seq(value))

  def withBody[T: ResponseWritable](body: T): Response[T] =
    Response[String].withBody(body)

  def withBodyOpt[T: ResponseWritable](body: Option[T], name: String): Response[T] = body match
    case Some(value) => withBody(value)
    case None        => throw NotFoundException(name)

  def redirect(location: String): Response[String] =
    withStatus(StatusCodes.MOVED_PERMANENTLY).withHeader("Location", location)

}
