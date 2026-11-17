package ba.sake.sharaf.handlers

import java.io.OutputStream
import java.util.Locale
import java.util.zip.GZIPOutputStream

import sttp.model.HeaderNames

import ba.sake.sharaf.*

final class GzipHandler(settings: GzipSettings, next: SharafHandler) extends SharafHandler {
  private val acceptEncodingHeader = HttpString(HeaderNames.AcceptEncoding)
  private val cacheControlHeader = HttpString(HeaderNames.CacheControl)
  private val contentEncodingHeader = HttpString(HeaderNames.ContentEncoding)
  private val contentLengthHeader = HttpString(HeaderNames.ContentLength)
  private val contentTypeHeader = HttpString(HeaderNames.ContentType)
  private val varyHeader = HttpString(HeaderNames.Vary)

  override def handle(context: RequestContext): Response[?] = {
    val response = next.handle(context)
    val bodyHeaders = response.body.toSeq.flatMap(response.rw.headers)
    if !hasCompressibleBody(response, bodyHeaders) then response
    else {
      val currentVary = response.headerUpdates.effective(varyHeader, bodyHeaders).getOrElse(Seq.empty)
      val updatedVary =
        if variesOnAcceptEncoding(currentVary) then currentVary
        else currentVary.appended("Accept-Encoding")
      val variedResponse =
        if updatedVary == currentVary then response
        else response.settingHeader(varyHeader, updatedVary)
      if !acceptsGzip(context.request.headerValues(acceptEncodingHeader)) then variedResponse
      else {
        val encodedBody = variedResponse.mapBody { (value, writer) =>
          val gzipBody: GzipBody = (
            headers = bodyHeaders,
            writeTo = output => {
              val gzip = new LevelGzipOutputStream(output, settings.bufferSize, settings.level)
              try {
                writer.write(value, gzip)
                gzip.finish()
              } finally gzip.endDeflater()
            }
          )
          gzipBody
        }
        encodedBody
          .removingHeader(contentLengthHeader)
          .settingHeader(contentEncodingHeader, "gzip")
      }
    }
  }

  private def hasCompressibleBody(response: Response[?], bodyHeaders: Seq[(HttpString, Seq[String])]): Boolean = {
    val hasBody = response.body.nonEmpty &&
      response.status.code != 204 &&
      response.status.code != 205 &&
      response.status.code != 304
    val contentType = headerValues(contentTypeHeader, response, bodyHeaders).headOption
    val hasContentEncoding = headerValues(contentEncodingHeader, response, bodyHeaders).nonEmpty
    val noTransform = headerValues(cacheControlHeader, response, bodyHeaders).exists { value =>
      value.toLowerCase(Locale.ROOT).contains("no-transform")
    }
    hasBody && !hasContentEncoding && !noTransform && contentType.exists(settings.isCompressibleMediaType)
  }

  private def headerValues(
      name: HttpString,
      response: Response[?],
      bodyHeaders: Seq[(HttpString, Seq[String])]
  ): Seq[String] =
    response.headerUpdates
      .effective(name, bodyHeaders)
      .getOrElse(Seq.empty)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def acceptsGzip(values: Option[Seq[String]]): Boolean = {
    var explicitQuality = Option.empty[Double]
    var wildcardQuality = Option.empty[Double]
    values.foreach { headerValues =>
      headerValues.foreach { value =>
        value.split(',').foreach { token =>
          val parts = token.split(';').map(_.trim)
          val coding = parts.headOption.getOrElse("").toLowerCase(Locale.ROOT)
          val qualityParameter = parts.iterator
            .drop(1)
            .find(_.toLowerCase(Locale.ROOT).startsWith("q="))
          val quality = qualityParameter match {
            case None => 1.0
            case Some(parameter) =>
              parameter
                .drop(2)
                .toDoubleOption
                .map(q => q.max(0.0).min(1.0))
                .getOrElse(0.0)
          }
          coding match {
            case "gzip" => explicitQuality = Some(quality)
            case "*"    => wildcardQuality = Some(quality)
            case _      => ()
          }
        }
      }
    }
    explicitQuality.orElse(wildcardQuality).exists(_ > 0.0)
  }

  private def variesOnAcceptEncoding(values: Seq[String]): Boolean =
    values.exists { value =>
      value.split(',').exists { token =>
        val normalized = token.trim
        normalized == "*" || normalized.equalsIgnoreCase("Accept-Encoding")
      }
    }
}

private type GzipBody = (
    headers: Seq[(HttpString, Seq[String])],
    writeTo: OutputStream => Unit
)

private given ResponseWritable[GzipBody] with {
  override def write(value: GzipBody, outputStream: OutputStream): Unit =
    value.writeTo(outputStream)

  override def headers(value: GzipBody): Seq[(HttpString, Seq[String])] =
    value.headers
}

private final class LevelGzipOutputStream(outputStream: OutputStream, bufferSize: Int, level: Int)
    extends GZIPOutputStream(outputStream, bufferSize) {
  `def`.setLevel(level)

  def endDeflater(): Unit = `def`.end()
}
