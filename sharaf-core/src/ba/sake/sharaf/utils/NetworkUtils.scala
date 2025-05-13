package ba.sake.sharaf.utils

import java.net.ServerSocket
import scala.util.Using

object NetworkUtils {
  def getFreePort(): Int =
    Using.resource(ServerSocket(0)) { ss =>
      ss.getLocalPort
    }
}
