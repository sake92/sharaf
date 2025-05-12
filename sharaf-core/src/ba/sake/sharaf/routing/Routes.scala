package ba.sake.sharaf.routing

import ba.sake.sharaf.{HttpMethod, Request, Response}

type RequestParams = (HttpMethod, Path)

type RoutesDefinition = Request ?=> PartialFunction[RequestParams, Response[?]]

// this is to make compiler happy at routes construction time... def apply doesnt work
class Routes(val definition: RoutesDefinition)

object Routes:
  def merge(routesDefinitions: Seq[Routes]): Routes = {
    val res: RoutesDefinition = routesDefinitions.map(_.definition).reduceLeft { case (acc, next) =>
      acc.orElse(next)
    }
    Routes(res)
  }
