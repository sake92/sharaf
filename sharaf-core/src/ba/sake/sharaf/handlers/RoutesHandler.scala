package ba.sake.sharaf.handlers

import ba.sake.sharaf.*

class RoutesHandler(routes: Routes, notFoundHandler: SharafHandler) extends SharafHandler:
  override def handle(context: RequestContext): Response[?] =
    given Request = context.request
    val routesDefinition = routes.definition
    routesDefinition.lift(context.params) match
      case Some(response) => response
      case None           => notFoundHandler.handle(context)
