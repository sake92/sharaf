package ba.sake.sharaf.routing

import ba.sake.sharaf.*

object q {
  def unapply[T](qs: QueryString)(using fqs: FromQueryString[T]): Option[T] =
    fqs.bind(qs.params)
}
