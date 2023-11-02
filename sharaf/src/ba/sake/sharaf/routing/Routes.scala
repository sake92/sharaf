package ba.sake.sharaf.routing

import ba.sake.sharaf.Request
import ba.sake.sharaf.Response

type RoutesDefinition = Request ?=> PartialFunction[RequestParams, Response[?]]

class Routes(routesDef: RoutesDefinition) {
  private[sharaf] def definition: RoutesDefinition = routesDef
}

object Routes {
  def merge(routess: Seq[Routes]): Routes =
    val routesDef: RoutesDefinition = routess.map(_.definition).reduceLeft { case (acc, next) =>
      acc.orElse(next)
    }
    Routes(routesDef)
}
