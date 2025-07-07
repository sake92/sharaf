package ba.sake.sharaf

import scala.util.hashing.MurmurHash3

// TODO implicit conversion from String ??
/** Case-insensitive string for HTTP headers and such.
  */
final class HttpString private (val value: String) {

  override def equals(other: Any): Boolean = other match {
    case h: AnyRef if this.eq(h) => true
    case that: HttpString        => value.equalsIgnoreCase(that.value)
    case _                       => false
  }

  override def hashCode(): Int = MurmurHash3.stringHash(value.toLowerCase)

  override def toString: String = value
}

object HttpString {
  def apply(value: String): HttpString = new HttpString(value)
}
