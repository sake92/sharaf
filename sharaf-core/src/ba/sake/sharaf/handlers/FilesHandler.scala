package ba.sake.sharaf.handlers

import java.nio.file.{Files, Path}
import java.security.MessageDigest
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.util.Try
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*
import sttp.model.*
import scala.util.boundary

final class FilesHandler(
    rootPath: Path,
    notFoundHandler: SharafHandler,
    enableCaching: Boolean = true,
    maxCacheAge: Long = FilesHandler.DefaultMaxCacheAge,
    enableRangeRequests: Boolean = true
) extends SharafHandler {

  private val absoluteRoot = rootPath.toAbsolutePath.normalize()
  if !Files.exists(absoluteRoot) then throw IllegalArgumentException(s"Root path does not exist: $absoluteRoot")
  if !Files.isDirectory(absoluteRoot) then
    throw IllegalArgumentException(s"Root path is not a directory: $absoluteRoot")

  override def handle(context: RequestContext): Response[?] =
    given Request = context.request
    val (method, path) = context.params
    if method != HttpMethod.GET && method != HttpMethod.HEAD then
      throw MethodNotAllowedException("Invalid method", s"Only GET and HEAD are supported, got: $method")
    val withBody = method == HttpMethod.GET
    val relativePath = path.segments.mkString("/").stripPrefix("/")
    try serve(relativePath, withBody)
    catch {
      case _: NotFoundException => notFoundHandler.handle(context)
    }

  private def serve(relativePath: String, withBody: Boolean)(using Request): Response[?] = {
    val filePath = resolvePath(relativePath)
    if !Files.exists(filePath) then throw NotFoundException("File")
    else if !Files.isRegularFile(filePath) then throw RejectedException("Invalid path", "Not a regular file")
    else if !Files.isReadable(filePath) then throw RejectedException("Invalid path", "File not readable")
    else serveFile(filePath, withBody)
  }

  /** Resolves and validates a path, preventing directory traversal attacks etc.
    */
  private def resolvePath(relativePath: String): Path = {
    val cleanPath = relativePath.stripPrefix("/")
    val isSuspicious = cleanPath.contains("..") ||
      cleanPath.contains("~") ||
      cleanPath.startsWith("/") ||
      cleanPath.contains("\\")
    if isSuspicious then throw RejectedException("Invalid path", "Suspicious path detected")
    val resolvedPath = absoluteRoot.resolve(cleanPath).normalize().toAbsolutePath
    if !resolvedPath.startsWith(absoluteRoot) then throw RejectedException("Invalid path", "Path traversal attempt")
    resolvedPath
  }

  private def serveFile(filePath: Path, withBody: Boolean)(using request: Request): Response[?] = boundary {
    val fileSize = Files.size(filePath)
    val lastModified = Files.getLastModifiedTime(filePath).toInstant
    val mimeType = detectMimeType(filePath)

    val etag = Option.when(enableCaching) {
      generateETag(filePath.toString, fileSize, lastModified)
    }
    etag.filter(_ => enableCaching).foreach { tag =>
      request.headers.get(HttpString(HeaderNames.IfNoneMatch)) match {
        case Some(Seq(clientETag, _)) if clientETag == tag =>
          boundary.break(
            Response
              .withStatus(StatusCode.NotModified)
              .settingHeader("ETag", tag)
          )
        case _ =>
      }
    }
    /*
    val rangeHeader =
      request.headers.get(HttpString(HeaderNames.Range)).filter(_ => enableRangeRequests).flatMap(_.headOption)
    rangeHeader match {
      case Some(range) if range.startsWith("bytes=") =>
        serveRangeRequest(filePath, fileSize, range, mimeType, etag, lastModified)
      case _ =>
        serveFullFile(filePath, fileSize, mimeType, etag, lastModified)
    }*/

    serveFullFile(filePath, fileSize, mimeType, etag, lastModified, withBody)
  }

  private def serveFullFile(
      filePath: Path,
      fileSize: Long,
      mimeType: String,
      etag: Option[String],
      lastModified: Instant,
      withBody: Boolean
  ): Response[?] = {
    var headers = Seq(
      "Content-Type" -> mimeType,
      "Content-Length" -> fileSize.toString,
      "Last-Modified" -> formatHttpDate(lastModified),
      "Accept-Ranges" -> "bytes"
    )
    if enableCaching && etag.isDefined then
      headers = headers.appended("ETag" -> etag.get)
      headers = headers.appended("Cache-Control" -> s"public, max-age=$maxCacheAge")
    else headers = headers.appended("Cache-Control" -> "no-cache, no-store, must-revalidate")
    val finalHeaders = headers.map((k, v) => (HttpString(k), Seq(v)))
    if withBody then Response.withBody(filePath).settingHeaders(finalHeaders)
    else Response.default.settingHeaders(finalHeaders)
  }

  /** Serves a partial file based on Range header.
    */
  /*
  private def serveRangeRequest(
      filePath: Path,
      fileSize: Long,
      rangeHeader: String,
      mimeType: String,
      etag: Option[String],
      lastModified: Instant
  ): Response = {
    parseRange(rangeHeader, fileSize) match {
      case Some((start, end)) =>
        val length = end - start + 1
        val rangeBytes = new Array[Byte](length.toInt)

        val fis = new FileInputStream(filePath.toFile)
        try {
          fis.skip(start)
          fis.read(rangeBytes)
        } finally {
          fis.close()
        }

        var response = Response
          .withStatus(StatusCode.PartialContent)
          .withBody(rangeBytes)
          .withHeader("Content-Type", mimeType)
          .withHeader("Content-Length", length.toString)
          .withHeader("Content-Range", s"bytes $start-$end/$fileSize")
          .withHeader("Last-Modified", formatHttpDate(lastModified))
          .withHeader("Accept-Ranges", "bytes")

        if (etag.isDefined) {
          response = response.withHeader("ETag", etag.get)
        }

        response

      case None =>
        // Invalid range
        Response
          .withStatus(StatusCode.RangeNotSatisfiable)
          //.withHeader("Content-Range", s"bytes /$fileSize")
          .withBody("Invalid range request")
    }
  }*/

  /** Parses a Range header (e.g., "bytes=0-1023").
    */
  private def parseRange(rangeHeader: String, fileSize: Long): Option[(Long, Long)] = {
    val rangePattern = "bytes=(\\d*)-(\\d*)".r
    rangeHeader match {
      case rangePattern(startStr, endStr) =>
        try {
          val start = if (startStr.isEmpty) 0L else startStr.toLong
          val end = if (endStr.isEmpty) fileSize - 1 else endStr.toLong.min(fileSize - 1)

          if (start >= fileSize || start > end) {
            None
          } else {
            Some((start, end))
          }
        } catch {
          case _: NumberFormatException => None
        }
      case _ => None
    }
  }

  private def detectMimeType(filePath: Path): String =
    Try(Files.probeContentType(filePath)).toOption
      .getOrElse("application/octet-stream")

  private def generateETag(path: String, size: Long, lastModified: Instant): String = {
    val data = s"$path-$size-${lastModified.toEpochMilli}"
    val md = MessageDigest.getInstance("MD5")
    val hash = md.digest(data.getBytes("UTF-8"))
    s""""${hash.map("%02x".format(_)).mkString}""""
  }

  /** Formats an Instant as HTTP date (RFC 7231).
    */
  private def formatHttpDate(instant: Instant): String = {
    val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
    ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")).format(formatter)
  }
}

object FilesHandler {

  val DefaultMaxCacheAge: Long = 86400 // 24 hours
}
