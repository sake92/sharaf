package ba.sake.sharaf

import io.undertow.util.HttpString

/** Headers represented as a series of immutable transformations. This is handy when you dynamically remove header(s),
  * maybe set by a previous Undertow handler.
  *
  * @param updates
  *   Series of header transformations
  */
private[sharaf] final case class HeaderUpdates(updates: Seq[HeaderUpdate]) {

  def setting(name: HttpString, values: Seq[String]): HeaderUpdates =
    copy(updates = updates.appended(HeaderUpdate.Set(name, values)))

  def setting(name: HttpString, value: String): HeaderUpdates =
    copy(updates = updates.appended(HeaderUpdate.Set(name, Seq(value))))

  def removing(name: HttpString): HeaderUpdates =
    copy(updates = updates.appended(HeaderUpdate.Remove(name)))

}

private[sharaf] enum HeaderUpdate:
  case Set(name: HttpString, values: Seq[String])
  case Remove(name: HttpString)
