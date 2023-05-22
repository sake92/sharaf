package ba.sake.sharaf.handlers

import io.undertow.server.HttpHandler
import io.undertow.server.handlers.resource.ResourceHandler
import ba.sake.sharaf.Routes
import io.undertow.server.handlers.resource.ClassPathResourceManager

object SharafHandler {
  def apply(routes: Routes): HttpHandler =
    ResourceHandler(
      ClassPathResourceManager(SharafHandler.getClass.getClassLoader, "resources/static"),
      RoutesHandler(routes)
    )

}
