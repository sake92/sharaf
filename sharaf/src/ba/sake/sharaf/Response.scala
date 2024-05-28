package ba.sake.sharaf

import io.undertow.util.StatusCodes
import io.undertow.util.HttpString

final class Response[T] private (
    val status: Int,
    private[sharaf] val headerUpdates: HeaderUpdates,
    val body: Option[T]
)(using val rw: ResponseWritable[T]) {

  def withStatus(status: Int): Response[T] =
    copy(status = status)

  def settingHeader(name: HttpString, values: Seq[String]): Response[T] =
    copy(headerUpdates = headerUpdates.setting(name, values))
  def settingHeader(name: String, values: Seq[String]): Response[T] =
    settingHeader(HttpString(name), values)
  def settingHeader(name: HttpString, value: String): Response[T] =
    copy(headerUpdates = headerUpdates.setting(name, value))
  def settingHeader(name: String, value: String): Response[T] =
    settingHeader(HttpString(name), value)

  def removingHeader(name: HttpString): Response[T] =
    copy(headerUpdates = headerUpdates.removing(name))
  def removingHeader(name: String): Response[T] =
    removingHeader(HttpString(name))

  def withBody[T2: ResponseWritable](body: T2): Response[T2] =
    copy(body = Some(body))

  private def copy[T2](
      status: Int = status,
      headerUpdates: HeaderUpdates = headerUpdates,
      body: Option[T2] = body
  )(using ResponseWritable[T2]) = new Response(status, headerUpdates, body)
}

object Response {

  private val defaultRes = new Response[String](StatusCodes.OK, HeaderUpdates(Seq.empty), None)

  def apply[T: ResponseWritable] = defaultRes

  def withStatus(status: Int) =
    defaultRes.withStatus(status)

  def settingHeader(name: HttpString, values: Seq[String]) =
    defaultRes.settingHeader(name, values)

  def settingHeader(name: HttpString, value: String) =
    defaultRes.settingHeader(name, Seq(value))

  def withBody[T: ResponseWritable](body: T): Response[T] =
    defaultRes.withBody(body)

  def withBodyOpt[T: ResponseWritable](body: Option[T], name: String): Response[T] = body match
    case Some(value) => withBody(value)
    case None        => throw exceptions.NotFoundException(name)

  def redirect(location: String): Response[String] =
    withStatus(StatusCodes.MOVED_PERMANENTLY).settingHeader(HttpString("Location"), location)

}
