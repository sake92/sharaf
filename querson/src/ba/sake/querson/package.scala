package ba.sake.querson

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val DefaultQuersonConfig = Config(SeqWriteMode.Brackets, ObjWriteMode.Dots)

extension (queryStringMap: QueryStringMap) {

  /** Parses `T` from a QueryStringMap map.
    */
  def parseQueryStringMap[T](using rw: QueryStringRW[T]): T =
    val obj = parseQSMap(queryStringMap)
    rw.parse("", obj)
}

extension [T](value: T)(using rw: QueryStringRW[T]) {

  /** Serializes `T` to QueryStringMap map. Note that values are **not** URL encoded.
    *
    * @param rw
    *   Typeclass that does the heavy lifting
    * @param config
    *   Configures how to serialize sequences and nested objects
    * @return
    *   QueryStringMap
    */
  def toQueryStringMap(config: Config = DefaultQuersonConfig): QueryStringMap =
    val qsData = rw.write("", value)
    writeToQSMap("", qsData, config)

    /** Serializes `T` to query string, with key/values URL encoded.
      *
      * @param rw
      *   Typeclass that does the heavy lifting
      * @param config
      *   Configures how to serialize sequences and nested objects
      * @return
      *   Query parameters string
      */
  def toQueryString(config: Config = DefaultQuersonConfig): String =
    val qsMap = toQueryStringMap(config)
    qsMap
      .flatMap { case (k, values) =>
        values.map { v =>
          val encodedKey = URLEncoder.encode(k, StandardCharsets.UTF_8)
          val encodedValue = URLEncoder.encode(v, StandardCharsets.UTF_8)
          s"$encodedKey=$encodedValue"
        }
      }
      .mkString("&")
}

case class Config(seqWriteMode: SeqWriteMode, objWriteMode: ObjWriteMode) {
  def withSeqBrackets = copy(seqWriteMode = SeqWriteMode.Brackets)
  def withSeqNoBrackets = copy(seqWriteMode = SeqWriteMode.NoBrackets)
  def withSeqEmptyBrackets = copy(seqWriteMode = SeqWriteMode.EmptyBrackets)

  def withObjBrackets = copy(objWriteMode = ObjWriteMode.Brackets)
  def withObjDots = copy(objWriteMode = ObjWriteMode.Dots)
}

enum SeqWriteMode:
  case Brackets, NoBrackets, EmptyBrackets

enum ObjWriteMode:
  case Brackets, Dots

/* exceptions */
sealed class QuersonException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

final class ParsingException(val errors: Seq[ParseError])
    extends QuersonException(
      "Query string parsing error: " + errors.map(_.text).mkString("; ")
    )
object ParsingException {
  def apply(errors: Seq[ParseError]): ParsingException =
    new ParsingException(errors)
  def apply(pe: ParseError): ParsingException =
    new ParsingException(Seq(pe))
}

case class ParseError(
    path: String,
    msg: String,
    value: Option[Any] = None
) {
  def withPath(p: String) = copy(path = p)
  def withValue(v: Any) = copy(value = Some(v))

  def text: String = value match {
    case Some(v) => s"Key '$path' with value '$v' $msg"
    case None    => s"Key '$path' $msg"
  }
}
