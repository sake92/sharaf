package ba.sake.sharaf.handlers

import ba.sake.sharaf.*
import ba.sake.sharaf.exceptions.NotFoundException

class RoutesHandler(routes: Routes) extends SharafHandler:
  override def handle(context: RequestContext): Response[?] =
    given Request = context.request
    val routesDefinition = routes.definition
    routesDefinition
      .lift(context.params)
      .getOrElse(throw NotFoundException("route"))
