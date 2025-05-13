package ba.sake.sharaf

// TODO implicit conversion from String ??
/** Case-insensitive string for HTTP headers and such.
  */
final class HttpString private (val value: String) {

  override def equals(other: Any): Boolean = other match {
    case that: HttpString => value.equalsIgnoreCase(that.value)
    case _                => false
  }

  override def toString: String = value
}

object HttpString {
  def apply(value: String): HttpString = new HttpString(value)
}
