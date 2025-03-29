package ba.sake.sharaf.utils

import java.net.ServerSocket
import scala.util.Using
import ba.sake.{formson, querson}

def getFreePort(): Int =
  Using.resource(ServerSocket(0)) { ss =>
    ss.getLocalPort
  }

// requests integration
extension [T](value: T)(using rw: formson.FormDataRW[T])
  def toRequestsMultipart(config: formson.Config = formson.DefaultFormsonConfig): requests.MultiPart =
    import formson.*
    val multiItems = value.toFormDataMap().flatMap { case (key, values) =>
      values.map {
        case FormValue.Str(value)       => requests.MultiItem(key, value)
        case FormValue.File(value)      => requests.MultiItem(key, value, value.getFileName.toString)
        case FormValue.ByteArray(value) => requests.MultiItem(key, value)
      }
    }
    requests.MultiPart(multiItems.toSeq*)

extension [T](value: T)(using rw: querson.QueryStringRW[T])
  def toRequestsQuery(config: querson.Config = querson.DefaultQuersonConfig): Map[String, String] =
    import querson.*
    value.toQueryStringMap().map { (k, vs) => k -> vs.head }
