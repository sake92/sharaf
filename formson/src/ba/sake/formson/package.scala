package ba.sake.formson

val DefaultConfig = Config(SeqWriteMode.Brackets, ObjWriteMode.Brackets)

extension (formDataMap: FormDataMap) {

  /** Parses `T` from a FormDataMap map.
    */
  def parseFormDataMap[T](using rw: FormDataRW[T]): T =
    val obj = parseFDMap(formDataMap)
    rw.parse("", obj)
}

extension [T](value: T)(using rw: FormDataRW[T]) {

  /** Serializes `T` to FormDataMap map. Note that key/values are **not** URL encoded.
    *
    * @param rw
    *   Typeclass that does the heavy lifting
    * @param config
    *   Configures how to serialize sequences and nested objects
    * @return
    *   FormDataMap
    */
  def toFormDataMap(config: Config = DefaultConfig): FormDataMap =
    val formData = rw.write("", value)
    writeToFDMap("", formData, config)

}

case class Config(seqWriteMode: SeqWriteMode, objWriteMode: ObjWriteMode)

enum SeqWriteMode:
  case NoBrackets, EmptyBrackets, Brackets

enum ObjWriteMode:
  case Brackets, Dots

/* exceptions */
sealed class FormsonException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

final class ParsingException(val errors: Seq[ParseError])
    extends FormsonException(
      errors
        .map(_.text)
        .mkString("; ")
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
