package ba.sake.sharaf

final class GzipSettings private (
    val level: Int,
    val bufferSize: Int
) {

  require(level >= 1 && level <= 9, "Gzip level must be between 1 and 9")
  require(bufferSize > 0, "Gzip buffer size must be positive")

  def withLevel(level: Int): GzipSettings = copy(level = level)

  def withBufferSize(bufferSize: Int): GzipSettings = copy(bufferSize = bufferSize)

  private def copy(
      level: Int = level,
      bufferSize: Int = bufferSize
  ): GzipSettings = new GzipSettings(level, bufferSize)
}

object GzipSettings {

  /** Standard gzip compression with an 8 KiB output buffer.
    *
    * Lower levels trade compression ratio for less CPU time per response.
    */
  val default: GzipSettings = new GzipSettings(
    level = 6,
    bufferSize = 8 * 1024
  )
}
