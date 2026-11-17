package ba.sake.sharaf

import java.util.Locale

final class GzipSettings private (
    val level: Int,
    val bufferSize: Int,
    val compressibleMediaTypes: Set[String]
) {

  require(level >= 1 && level <= 9, "Gzip level must be between 1 and 9")
  require(bufferSize > 0, "Gzip buffer size must be positive")
  require(compressibleMediaTypes.nonEmpty, "At least one compressible media type is required")
  require(
    compressibleMediaTypes.forall(GzipSettings.isValidMediaType),
    "Compressible media types must contain a type and subtype"
  )

  def withLevel(level: Int): GzipSettings = copy(level = level)

  def withBufferSize(bufferSize: Int): GzipSettings = copy(bufferSize = bufferSize)

  def withCompressibleMediaTypes(mediaTypes: Set[String]): GzipSettings =
    copy(compressibleMediaTypes = mediaTypes)

  private[sharaf] def isCompressibleMediaType(contentType: String): Boolean = {
    val mediaType = contentType.takeWhile(_ != ';').trim.toLowerCase(Locale.ROOT)
    mediaType != "text/event-stream" && compressibleMediaTypes.contains(mediaType)
  }

  private def copy(
      level: Int = level,
      bufferSize: Int = bufferSize,
      compressibleMediaTypes: Set[String] = compressibleMediaTypes
  ): GzipSettings =
    new GzipSettings(
      level = level,
      bufferSize = bufferSize,
      compressibleMediaTypes = GzipSettings.normalizeMediaTypes(compressibleMediaTypes)
    )
}

object GzipSettings {

  private val defaultCompressibleMediaTypes = Set(
    "text/html",
    "text/plain",
    "text/css",
    "text/csv",
    "text/xml",
    "text/javascript",
    "application/json",
    "application/xml",
    "application/javascript",
    "application/x-javascript",
    "application/rtf",
    "image/svg+xml"
  )

  /** Standard gzip compression with an 8 KiB output buffer and common compressible media types.
    *
    * Lower levels trade compression ratio for less CPU time per response.
    */
  val default: GzipSettings = new GzipSettings(
    level = 6,
    bufferSize = 8 * 1024,
    compressibleMediaTypes = defaultCompressibleMediaTypes
  )

  private def normalizeMediaTypes(mediaTypes: Set[String]): Set[String] =
    mediaTypes.map(_.trim.toLowerCase(Locale.ROOT))

  private def isValidMediaType(value: String): Boolean = {
    val normalized = value.trim.toLowerCase(Locale.ROOT)
    val slash = normalized.indexOf('/')
    slash > 0 &&
    slash < normalized.length - 1 &&
    normalized.indexOf('/', slash + 1) < 0
  }
}
