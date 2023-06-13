package ba.sake.sharaf.routing

import scala.util.Try

import ba.sake.querson.*

object q {
  def unapply[T](qs: RawQueryString)(using fqs: QueryStringRW[T]): Option[T] = {
    Try(qs.parseQueryString[T]).toOption
  }
}
