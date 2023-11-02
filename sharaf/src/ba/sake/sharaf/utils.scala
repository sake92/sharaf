package ba.sake.sharaf.utils

import java.net.ServerSocket
import scala.util.Using
import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions

import ba.sake.formson._
import ba.sake.tupson._
import ba.sake.querson.QueryStringMap

def getFreePort(): Int =
  Using.resource(ServerSocket(0)) { ss =>
    ss.getLocalPort()
  }

// requests integration
extension (formDataMap: FormDataMap)
  def toRequestsMultipart() =
    val multiItems = formDataMap.flatMap { case (key, values) =>
      values.map {
        case FormValue.Str(value)       => requests.MultiItem(key, value)
        case FormValue.File(value)      => requests.MultiItem(key, value, value.getFileName.toString)
        case FormValue.ByteArray(value) => requests.MultiItem(key, value)
      }
    }
    requests.MultiPart(multiItems.toSeq*)

extension (queryStringMap: QueryStringMap)
  def toRequestsQuery(): Map[String, String] =
    queryStringMap.map { (k, vs) => k -> vs.head }

// typesafe config easy parsing
extension (config: Config) {
  def parse[T: JsonRW]() =
    val configJsonString = config
      .root()
      .render(
        ConfigRenderOptions.concise().setJson(true)
      )
    configJsonString.parseJson[T]

}
