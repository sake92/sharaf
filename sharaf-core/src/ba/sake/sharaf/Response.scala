package ba.sake.sharaf

import sttp.model.StatusCode

final class Response[T] private (
    val status: StatusCode,
    private[sharaf] val headerUpdates: HeaderUpdates,
    private[sharaf] val cookieUpdates: CookieUpdates,
    val body: Option[T]
)(using val rw: ResponseWritable[T]) {

  def withStatus(status: StatusCode): Response[T] =
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

  def settingCookie(value: Cookie): Response[T] =
    copy(cookieUpdates = cookieUpdates.setting(value))
  def removingCookie(name: String): Response[T] =
    copy(cookieUpdates = cookieUpdates.removing(name))

  def withBody[T2: ResponseWritable](body: T2): Response[T2] =
    copy(body = Some(body))

  private def copy[T2](
      status: StatusCode = status,
      headerUpdates: HeaderUpdates = headerUpdates,
      cookieUpdates: CookieUpdates = cookieUpdates,
      body: Option[T2] = body
  )(using ResponseWritable[T2]) = new Response(status, headerUpdates, cookieUpdates, body)

  override def toString(): String =
    s"Response(status=$status, headerUpdates=${headerUpdates}, cookieUpdates=${cookieUpdates}, body=...)"
}

object Response {

  private val LocationHeader = HttpString("Location")

  val default: Response[String] =
    new Response[String](StatusCode.Ok, HeaderUpdates(Seq.empty), CookieUpdates(Seq.empty), None)

  def withStatus(status: StatusCode): Response[String] =
    default.withStatus(status)

  def settingHeader(name: HttpString, values: Seq[String]): Response[String] =
    default.settingHeader(name, values)
  def settingHeader(name: String, values: Seq[String]): Response[String] =
    default.settingHeader(name, values)
  def settingHeader(name: HttpString, value: String): Response[String] =
    default.settingHeader(name, Seq(value))
  def settingHeader(name: String, value: String): Response[String] =
    default.settingHeader(name, value)
  def removingHeader(name: HttpString): Response[String] =
    default.removingHeader(name)
  def removingHeader(name: String): Response[String] =
    default.removingHeader(name)

  def settingCookie(value: Cookie): Response[String] =
    default.settingCookie(value)
  def removingCookie(name: String): Response[String] =
    default.removingCookie(name)

  def withBody[T: ResponseWritable](body: T): Response[T] =
    default.withBody(body)

  def withBodyOpt[T2: ResponseWritable](body: Option[T2], name: String): Response[T2] = body match
    case Some(value) => withBody(value)
    case None        => throw exceptions.NotFoundException(name)

  def redirect(location: String): Response[String] =
    default.withStatus(StatusCode.MovedPermanently).settingHeader(LocationHeader, location)

}
