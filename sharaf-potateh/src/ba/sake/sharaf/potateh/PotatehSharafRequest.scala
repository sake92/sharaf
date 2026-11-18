package ba.sake.sharaf.potateh

import java.nio.charset.StandardCharsets.UTF_8
import ba.sake.formson.*
import ba.sake.querson.*
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.Path

final class PotatehSharafRequest(val underlying: ba.sake.potateh.Request) extends Request {

  private val uploads = new MultipartUploads
  private[sharaf] def closeUploads(): Unit = uploads.close()

  private[potateh] def path: Path =
    RequestDecoding.path(underlying.rawPath.getOrElse(""))

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
    UrlEncoded.query(underlying.rawQuery.getOrElse(""))

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
