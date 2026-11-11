package ba.sake.sharaf.handlers

import java.util.Locale
import java.nio.file.Path

private[handlers] object FileMimeTypes {
  private val types = Map(
    "html" -> "text/html",
    "htm" -> "text/html",
    "css" -> "text/css",
    "js" -> "text/javascript",
    "mjs" -> "text/javascript",
    "json" -> "application/json",
    "txt" -> "text/plain",
    "xml" -> "application/xml",
    "svg" -> "image/svg+xml",
    "png" -> "image/png",
    "jpg" -> "image/jpeg",
    "jpeg" -> "image/jpeg",
    "gif" -> "image/gif",
    "webp" -> "image/webp",
    "ico" -> "image/x-icon",
    "woff" -> "font/woff",
    "woff2" -> "font/woff2",
    "ttf" -> "font/ttf",
    "otf" -> "font/otf",
    "pdf" -> "application/pdf"
  )

  def forPath(path: Path): String = {
    val name = path.getFileName.toString
    val extension = name.lastIndexOf('.') match {
      case -1    => ""
      case index => name.substring(index + 1).toLowerCase(Locale.ROOT)
    }
    types.getOrElse(extension, "application/octet-stream")
  }
}
