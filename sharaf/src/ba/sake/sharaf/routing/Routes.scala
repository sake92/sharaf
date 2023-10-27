package ba.sake.sharaf.routing

import ba.sake.sharaf.Request
import ba.sake.sharaf.Response

type Routes = Request ?=> PartialFunction[RequestParams, Response[?]]

object Routes {
  def merge(routess: Seq[Routes]): Routes =
    routess.reduceLeft { case (acc, next) =>
      acc.orElse(next)
    }
}
