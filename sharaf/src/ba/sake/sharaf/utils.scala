package ba.sake.sharaf

import java.net.ServerSocket
import scala.util.Using

object SharafUtils:

  def getFreePort(): Int =
    Using.resource(new ServerSocket(0)) { ss =>
      ss.getLocalPort()
    }
