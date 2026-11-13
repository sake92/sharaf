package ba.sake.sharaf

private[sharaf] object KnownBodyLength {

  def of(value: Any): Option[Long] = value match {
    case text: String       => Some(utf8Length(text))
    case bytes: Array[Byte] => Some(bytes.length.toLong)
    case _                  => None
  }

  private def utf8Length(value: String): Long = {
    var chars = 0
    var bytes = 0L
    while (chars < value.length) {
      val current = value.charAt(chars)
      if (current <= 0x7f) {
        bytes += 1
      } else if (current <= 0x7ff) {
        bytes += 2
      } else if (Character.isHighSurrogate(current)) {
        val next = chars + 1
        if (next < value.length && Character.isLowSurrogate(value.charAt(next))) {
          bytes += 4
          chars += 1
        } else {
          bytes += 1
        }
      } else if (Character.isLowSurrogate(current)) {
        bytes += 1
      } else {
        bytes += 3
      }
      chars += 1
    }
    bytes
  }
}
