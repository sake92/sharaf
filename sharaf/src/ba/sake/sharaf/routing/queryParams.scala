package ba.sake.sharaf.routing

import scala.util.Try

import ba.sake.querson.*

// TODO remove in favor of Request.queryParams[T] :)))))))
object q {
  def unapply[T](qs: RawQueryString)(using fqs: QueryStringRW[T]): Option[T] = {
    Try(qs.parseRawQueryString[T]).toOption
  }
}
