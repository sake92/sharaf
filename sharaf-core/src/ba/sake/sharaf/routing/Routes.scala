package ba.sake.sharaf.routing

import ba.sake.sharaf.{HttpMethod, Request, Response}

type RequestParams = (HttpMethod, Path)

type SharafRoutesDefinition[Req <: Request] = Req ?=> PartialFunction[RequestParams, Response[?]]

// this is to make compiler happy at routes construction time... def apply doesnt work
class SharafRoutes[Req <: Request](val definition: SharafRoutesDefinition[Req])

object SharafRoutes:
  def merge[Req <: Request](routesDefinitions: Seq[SharafRoutes[Req]]): SharafRoutes[Req] = {
    val res: SharafRoutesDefinition[Req] = routesDefinitions.map(_.definition).reduceLeft { case (acc, next) =>
      acc.orElse(next)
    }
    SharafRoutes(res)
  }
