package ba.sake.sharaf.potateh

import java.io.InputStream
import ba.sake.querson.*
import ba.sake.sharaf.*

final class PotatehSharafRequest(val underlying: ba.sake.potateh.Request) extends MultipartRequest {

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

  override def bodyInputStream: InputStream = underlying.body

  override def toString(): String =
    "PotatehSharafRequest(...)"
}
