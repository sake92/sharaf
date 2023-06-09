package ba.sake.sharaf

import io.undertow.server.handlers.resource.ClassPathResourceManager
import io.undertow.server.handlers.resource.Resource as UResource

sealed trait Resource

object Resource {
  private val cprm = new ClassPathResourceManager(getClass.getClassLoader)

  def fromClassPath(path: String): Option[Resource] =
    Option(cprm.getResource(path)).map(ClasspathResource(_))

  private[sharaf] final class ClasspathResource(val underlying: UResource) extends Resource
}
