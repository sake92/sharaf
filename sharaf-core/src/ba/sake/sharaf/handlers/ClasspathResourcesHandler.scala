package ba.sake.sharaf.handlers

import java.net.{URL, URLConnection}
import java.security.MessageDigest
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.*
import sttp.model.*
import scala.util.boundary

final class ClasspathResourcesHandler(
    rootPath: String,
    notFoundHandler: SharafHandler,
    classLoader: ClassLoader = getClass.getClassLoader,
    enableCaching: Boolean = true,
    maxCacheAge: Long = ClasspathResourcesHandler.DefaultMaxCacheAge
) extends SharafHandler {

  // Normalize root path (remove leading/trailing slashes)
  private val normalizedRoot = rootPath.stripPrefix("/").stripSuffix("/")
  if normalizedRoot.isEmpty then throw IllegalArgumentException("Root path cannot be empty")

  println(s"Serving resources from: $normalizedRoot")

  override def handle(context: RequestContext): Response[?] =
    given Request = context.request
    val (method, path) = context.params
    if method != HttpMethod.GET && method != HttpMethod.HEAD then
      throw MethodNotAllowedException("Invalid method", s"Only GET and HEAD are supported, got: $method")
    val withBody = method == HttpMethod.GET
    val relativePath = path.segments.mkString("/").stripPrefix("/")
    val res =
      try serve(relativePath, withBody)
      catch {
        case _: NotFoundException => notFoundHandler.handle(context)
        case other =>
          println("SOME OTHER EXCEPTION IN CLASSPATH RESOURCES HANDLER:")
          other.printStackTrace()
          throw other
      }
    println(s"ClasspathResourcesHandler response: $res")
    res

  private def serve(relativePath: String, withBody: Boolean)(using Request): Response[?] = {
    val resourcePath = buildResourcePath(relativePath)
    val resourceUrl = getResourceUrl(resourcePath)
    serveResource(resourceUrl, resourcePath, withBody)
  }

  /** Builds the full resource path, preventing directory traversal.
    */
  private def buildResourcePath(relativePath: String): String = {
    val cleanPath = relativePath.stripPrefix("/")
    val isSuspicious = cleanPath.contains("..") ||
      cleanPath.contains("~") ||
      cleanPath.contains("\\") ||
      cleanPath.startsWith("/")
    if isSuspicious then throw RejectedException("Invalid path", "Suspicious path detected")
    s"$normalizedRoot/$cleanPath"
  }

  private def getResourceUrl(resourcePath: String): URL =
    Option(classLoader.getResource(resourcePath)) match {
      case Some(url) =>
        // Additional security check: ensure the resolved URL is actually for this resource
        // This prevents issues where getResource might return unexpected results
        validateResourceUrl(url, resourcePath)
        url
      case None =>
        println(s"Resource not found: $resourcePath")
        throw NotFoundException("Resource")
    }

  /** Validates that the URL actually points to the expected resource.
    */
  private def validateResourceUrl(url: URL, expectedPath: String): Unit = {
    val urlString = url.toString
    // For JAR URLs, ensure the path matches
    if urlString.contains("jar:") then
      val jarPath = urlString.split("!").lastOption.getOrElse("")
      if !jarPath.endsWith(expectedPath) then throw RejectedException("Invalid resource", "Resource path mismatch")
  }

  private def serveResource(resourceUrl: URL, resourcePath: String, withBody: Boolean)(using
      request: Request
  ): Response[?] = boundary {
    println(s"Serving resource: $resourceUrl ; $resourcePath")
    val connection = resourceUrl.openConnection()
    connection.setUseCaches(false)
    val contentLength = connection.getContentLengthLong
    val lastModified = connection.getLastModified match {
      case 0    => Instant.now() // Unknown modification time
      case time => Instant.ofEpochMilli(time)
    }

    val etag = Option.when(enableCaching) {
      generateETag(resourcePath, contentLength, lastModified)
    }
    etag.filter(_ => enableCaching).foreach { tag =>
      request.headers.get(HttpString(HeaderNames.IfNoneMatch)) match {
        case Some(Seq(clientETag, _*)) if clientETag == tag =>
          boundary.break(
            Response
              .withStatus(StatusCode.NotModified)
              .settingHeader("ETag", tag)
          )
        case _ =>
      }
    }

    val mimeType = detectMimeType(connection)

    serveFullResource(resourceUrl, contentLength, mimeType, etag, lastModified, withBody)
  }

  private def serveFullResource(
      resourceUrl: URL,
      contentLength: Long,
      mimeType: String,
      etag: Option[String],
      lastModified: Instant,
      withBody: Boolean
  ): Response[?] = {
    println(s"serveFullResource: $resourceUrl")
    var headers = Seq(
      "Content-Type" -> mimeType,
      "Last-Modified" -> formatHttpDate(lastModified)
    )

    if contentLength > 0 then headers = headers.appended("Content-Length" -> contentLength.toString)

    if enableCaching && etag.isDefined then
      headers = headers.appended("ETag" -> etag.get)
      headers = headers.appended("Cache-Control" -> s"public, max-age=$maxCacheAge")
    else headers = headers.appended("Cache-Control" -> "no-cache, no-store, must-revalidate")

    val finalHeaders = headers.map((k, v) => (HttpString(k), Seq(v)))

    println(s"Final headers: $finalHeaders")

    if withBody then Response.withBody(resourceUrl.openStream).settingHeaders(finalHeaders)
    else Response.default.settingHeaders(finalHeaders)
  }

  private def detectMimeType(connection: URLConnection): String =
    Option(connection.getContentType).filter(_.nonEmpty) match {
      case Some(contentType) => contentType
      case None              => "application/octet-stream"
    }

  private def generateETag(path: String, size: Long, lastModified: Instant): String = {
    val data = s"$path-$size-${lastModified.toEpochMilli}"
    val md = MessageDigest.getInstance("MD5")
    val hash = md.digest(data.getBytes("UTF-8"))
    s""""${hash.map("%02x".format(_)).mkString}""""
  }

  private def formatHttpDate(instant: Instant): String = {
    val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
    ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")).format(formatter)
  }
}

object ClasspathResourcesHandler {
  val DefaultMaxCacheAge: Long = 86400 // 24 hours
}
