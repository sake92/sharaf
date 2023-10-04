package ba.sake.sharaf

import java.net.ServerSocket
import scala.util.Using

import ba.sake.formson._

object SharafUtils:

  def getFreePort(): Int =
    Using.resource(new ServerSocket(0)) { ss =>
      ss.getLocalPort()
    }

  // requests integration
  extension (formDataMap: FormDataMap)
    def toRequestsMultipart() = {
      val multiItems = formDataMap.flatMap { case (key, values) =>
        values.map {
          case FormValue.Str(value)       => requests.MultiItem(key, value)
          case FormValue.File(value)      => requests.MultiItem(key, value, value.getFileName.toString)
          case FormValue.ByteArray(value) => requests.MultiItem(key, value)
        }
      }
      requests.MultiPart(multiItems.toSeq*)
    }
