// temporary until tupson supports it
package ba.sake.tupson

import java.net.*
import org.typelevel.jawn.ast.*

// java.net
// there is no RW for InetAddress because it could do host lookups.. :/
given JsonRW[URI] with {
  override def write(value: URI): JValue = JString(value.toString())
  override def parse(path: String, jValue: JValue): URI = jValue match
    case JString(s) => new URI(s)
    case other      => JsonRW.typeMismatchError(path, "URI", other)
}

given JsonRW[URL] with {
  override def write(value: URL): JValue = JString(value.toString())
  override def parse(path: String, jValue: JValue): URL = jValue match
    case JString(s) => new URI(s).toURL()
    case other      => JsonRW.typeMismatchError(path, "URL", other)
}
