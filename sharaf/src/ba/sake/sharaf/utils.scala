package ba.sake.sharaf.utils

import java.net.ServerSocket
import scala.util.Using
import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions

import ba.sake.formson
import ba.sake.tupson.*
import ba.sake.querson

def getFreePort(): Int =
  Using.resource(ServerSocket(0)) { ss =>
    ss.getLocalPort()
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

// typesafe config easy parsing
extension (config: Config) {
  def parse[T: JsonRW]() =
    ConfigUtils.parse(config)
}

private object ConfigUtils {
  import org.typelevel.jawn.ast.*

  def parse[T](config: Config)(using rw: JsonRW[T]) =
    val configJsonString = config
      .root()
      .render(
        ConfigRenderOptions.concise().setJson(true)
      )
    val jValue = JParser.parseUnsafe(configJsonString)
    adapt(jValue).toString.parseJson[T]

  // if you set a sys/env property,
  // the config cannot MAGICALLY know if it is a number or a string, so default is string, wack
  // so we adapt string to numbers if possible
  private def adapt(jvalue: JValue): JValue = jvalue match
    case JString(s) =>
      s.toLongOption match
        case Some(n) => JNum(n)
        case None =>
          s.toDoubleOption match
            case Some(d) => JNum(d)
            case None    => jvalue
    case JArray(vs) => JArray(vs.map(adapt))
    case JObject(vs) =>
      val adaptedMap = vs.map { (k, v) => k -> adapt(v) }
      JObject(adaptedMap)
    case _ => jvalue

}
