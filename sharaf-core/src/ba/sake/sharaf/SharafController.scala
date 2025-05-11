package ba.sake.sharaf

import ba.sake.sharaf.routing.SharafRoutes

trait SharafController[R <: Request]:
  def routes: SharafRoutes[R]
