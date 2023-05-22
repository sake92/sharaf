package ba.sake.sharaf

import java.nio.file.Path
import java.net.URI

enum Resource {
    case Classpath(path: String)
    case File(path: Path)
   // case URL(uri: URI)
}
