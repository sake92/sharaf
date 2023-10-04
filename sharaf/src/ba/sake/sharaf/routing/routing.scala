package ba.sake.sharaf
package routing

import java.util.UUID
import scala.util.Try

import io.undertow.util.HttpStringh

type RequestParams = (HttpString, Path)

type Routes = Request ?=> PartialFunction[RequestParams, Response[?]]

// typeclass for converting a path parameter to T
trait FromPathParam[T] {
  def extract(str: String): Option[T]
}

// TODO derive for simple enums
object FromPathParam {
  given FromPathParam[Int] = new {
    def extract(str: String): Option[Int] = str.toIntOption
  }
  given FromPathParam[Long] = new {
    def extract(str: String): Option[Long] = str.toLongOption
  }
  given FromPathParam[UUID] = new {
    def extract(str: String): Option[UUID] = Try(UUID.fromString(str)).toOption
  }
}

// nice extractors
// TODO redundant ???
final class UrlParamBinder[T](using fp: FromPathParam[T]) {
  def unapply(str: String): Option[T] =
    fp.extract(str)
}

val int = new UrlParamBinder[Int]
val long = new UrlParamBinder[Long]
val uuid = new UrlParamBinder[UUID]

// for custom params with FromPathParam tc impl
object param {
  def unapply[T](str: String)(using fp: FromPathParam[T]): Option[T] =
    fp.extract(str)
}
