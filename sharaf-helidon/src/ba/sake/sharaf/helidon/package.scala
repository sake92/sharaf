package ba.sake.sharaf.helidon

import java.io.OutputStream
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*

type HelidonSharafRoutes = SharafRoutes[HelidonSharafRequest]

object HelidonSharafRoutes:
  export SharafRoutes.merge
  def apply(routesDef: HelidonSharafRequest ?=> PartialFunction[RequestParams, Response[?]]): HelidonSharafRoutes =
    SharafRoutes(routesDef)

type HelidonSharafController = SharafController[HelidonSharafRequest]
