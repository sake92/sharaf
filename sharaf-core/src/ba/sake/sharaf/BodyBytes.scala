package ba.sake.sharaf

import java.io.InputStream
import java.util.Arrays

/** Reads a request body in full, sizing the buffer from Content-Length when it is declared.
  *
  * Returns the same bytes as `InputStream.readAllBytes`: the declared length only sizes the buffer, and reading
  * continues to end of stream. The JDK default allocates at least 16 KiB and then copies the result, even for an empty
  * body; with an accurate Content-Length this makes one exact allocation without a copy.
  */
private[sharaf] object BodyBytes {

  /** Largest buffer allocated from Content-Length before any bytes arrive, so a false length cannot force a large
    * allocation. Bodies longer than this grow as data arrives.
    */
  private[sharaf] val MaxPreallocatedBytes: Int = 64 * 1024

  /** Initial buffer when the length is not declared, e.g. for chunked bodies. */
  private[sharaf] val UndeclaredInitialBytes: Int = 1024

  private val MaxArrayBytes = Int.MaxValue - 8

  def read(input: InputStream, declaredLength: Option[Long]): Array[Byte] = {
    val initialCapacity = declaredLength match {
      case Some(length) => math.min(length, MaxPreallocatedBytes.toLong).toInt
      case None         => UndeclaredInitialBytes
    }
    var buffer = new Array[Byte](initialCapacity)
    var size = 0
    var done = false
    while !done do {
      if size == buffer.length then {
        // A full buffer may be the whole body: probe for end of stream before growing.
        val next = input.read()
        if next < 0 then done = true
        else {
          buffer = grow(buffer)
          buffer(size) = next.toByte
          size += 1
        }
      } else {
        val count = input.read(buffer, size, buffer.length - size)
        if count < 0 then done = true
        else size += count
      }
    }
    if size == buffer.length then buffer else Arrays.copyOf(buffer, size)
  }

  /** The declared body length, if Content-Length has exactly one valid value. Framing is validated by the server, so
    * any other value only means the length is unknown here.
    */
  def declaredLength(values: Option[Seq[String]]): Option[Long] = values match {
    case Some(Seq(text)) if text.nonEmpty && text.length <= 18 && text.forall(c => c >= '0' && c <= '9') =>
      Some(text.toLong)
    case _ => None
  }

  private def grow(buffer: Array[Byte]): Array[Byte] = {
    if buffer.length >= MaxArrayBytes then throw new OutOfMemoryError("Request body exceeds the maximum array size")
    val doubled = math.max(buffer.length.toLong * 2, UndeclaredInitialBytes.toLong)
    Arrays.copyOf(buffer, math.min(doubled, MaxArrayBytes.toLong).toInt)
  }
}
