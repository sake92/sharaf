package ba.sake.sharaf.routing

import java.util.UUID
import scala.util.Try
import io.undertow.util.Methods
import io.undertow.util.HttpString
import ba.sake.sharaf.QueryString

// typeclass for converting a path parameter OR query parameter to T
trait FromUrlParam[T] {
  def extract(str: String): Option[T]
}

object FromUrlParam {
  given FromUrlParam[Int] = new {
    def extract(str: String): Option[Int] = str.toIntOption
  }
  given FromUrlParam[Long] = new {
    def extract(str: String): Option[Long] = str.toLongOption
  }
  given FromUrlParam[UUID] = new {
    def extract(str: String): Option[UUID] = Try(UUID.fromString(str)).toOption
  }
}

// nice extractors
final class UrlParamBinder[T](using fp: FromUrlParam[T]) {
  def unapply(str: String): Option[T] =
    fp.extract(str)
}

val int = new UrlParamBinder[Int]
val long = new UrlParamBinder[Long]
val uuid = new UrlParamBinder[UUID]

// for custom params with FromUrlParam tc impl
object param {
  def unapply[T](str: String)(using fp: FromUrlParam[T]): Option[T] =
    fp.extract(str)
}

/* Query params */
object ? {
  def unapplySeq(queryString: QueryString): Option[Seq[(String, Seq[String])]] =
    Some(queryString.params)
}

object q {
  def unapply(values: Seq[String]): Option[String] =
    values.headOption
}

object qOpt {
  def unapply(values: Seq[String]): Option[Option[String]] =
    Option(values.headOption)
}

object qSeq {
  def unapply(values: Seq[String]): Option[Seq[String]] =
    Option(values)
}

/* HTTP methods */
object GET:
  def unapply[T](str: HttpString): Boolean =
    Methods.GET == str

object POST:
  def unapply[T](str: HttpString): Boolean =
    Methods.POST == str

object PUT:
  def unapply[T](str: HttpString): Boolean =
    Methods.PUT == str

object DELETE:
  def unapply[T](str: HttpString): Boolean =
    Methods.DELETE == str
