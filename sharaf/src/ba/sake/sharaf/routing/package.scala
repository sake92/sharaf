package ba.sake.sharaf
package routing

import io.undertow.util.HttpString

type RequestParams = (HttpString, Path)

type Routes = Request ?=> PartialFunction[RequestParams, Response[?]]

object param {
  def unapply[T](str: String)(using fp: FromPathParam[T]): Option[T] =
    fp.parse(str)
}
