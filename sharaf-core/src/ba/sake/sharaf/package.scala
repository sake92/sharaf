package ba.sake.sharaf

import sttp.client4.*
import sttp.model.*
import ba.sake.sharaf.routing.FromPathParam
import ba.sake.{formson, querson}
import formson.*
import querson.*

export HttpMethod.*

type ExceptionMapper = exceptions.ExceptionMapper
val ExceptionMapper = exceptions.ExceptionMapper

type Routes = ba.sake.sharaf.routing.Routes
val Routes = ba.sake.sharaf.routing.Routes

val Path = ba.sake.sharaf.routing.Path

object param:
  def unapply[T](str: String)(using fp: FromPathParam[T]): Option[T] =
    fp.parse(str)

// conversions to STTP
extension [T](value: T)(using rw: formson.FormDataRW[T])
  def toSttpMultipart(config: formson.Config = formson.DefaultFormsonConfig): Seq[Part[BasicBodyPart]] =
    val multiParts = value.toFormDataMap(config).flatMap { case (key, values) =>
      values.map {
        case formson.FormValue.Str(value)       => multipart(key, value)
        case formson.FormValue.File(value)      => multipartFile(key, value.toFile)
        case formson.FormValue.ByteArray(value) => multipart(key, value)
      }
    }
    multiParts.toSeq

extension [T](value: T)(using rw: querson.QueryStringRW[T])
  def toSttpQuery(config: querson.Config = querson.DefaultQuersonConfig): QueryParams =
    val params = value.toQueryStringMap(config).map { (k, vs) => k -> vs }
    QueryParams.fromMultiMap(params)
