package ba.sake.sharaf.potateh

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.Path
import ba.sake.sharaf.exceptions.BadRequestException

final class PotatehSharafRequest(val underlying: ba.sake.potateh.Request) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  private[potateh] lazy val uri: URI =
    try {
      val target = underlying.target
      val absolute = if target.startsWith("/") then "http://request.invalid" + target else target
      URI.create(absolute)
    } catch case _: IllegalArgumentException => throw BadRequestException("Invalid request target")

  private lazy val rawPathAndQuery: (rawPath: String, rawQuery: String) = {
    val target = underlying.target
    if target.startsWith("/") then {
      val queryStart = target.indexOf('?')
      if queryStart < 0 then (rawPath = target, rawQuery = "")
      else (rawPath = target.substring(0, queryStart), rawQuery = target.substring(queryStart + 1))
    } else {
      (
        rawPath = Option(uri.getRawPath).getOrElse(""),
        rawQuery = Option(uri.getRawQuery).getOrElse("")
      )
    }
  }

  private[potateh] def path: Path =
    RequestDecoding.path(rawPathAndQuery.rawPath)

  override lazy val headers: Map[HttpString, Seq[String]] =
    underlying.headers.fields.groupMap { case (name, _) => HttpString(name) } { case (_, value) => value }

  private[sharaf] override def headerValues(name: HttpString): Option[Seq[String]] = {
    val fields = underlying.headers.fields
    var index = 0
    var found = false
    val values = Vector.newBuilder[String]
    while index < fields.length do {
      val field = fields(index)
      if name.value.equalsIgnoreCase(field.name) then {
        found = true
        values += field.value
      }
      index += 1
    }
    if found then Some(values.result()) else None
  }

  override lazy val queryParamsRaw: QueryStringMap =
    UrlEncoded.query(rawPathAndQuery.rawQuery)

  override lazy val bodyString: String = {
    val contentType = underlying.headers.first("Content-Type").getOrElse("")
    if UrlEncoded.isForm(contentType) then UrlEncoded.readFormBody(underlying.body)
    else new String(underlying.body.readAllBytes(), UTF_8)
  }

  override lazy val bodyFormRaw: FormDataMap = {
    val contentType = underlying.headers.first("Content-Type").getOrElse("")
    if Multipart.isMultipart(contentType) then Multipart.parse(underlying.body, contentType, uploads)
    else super.bodyFormRaw
  }

}
