package ba.sake.querson

extension (rawQueryString: RawQueryString) {
  def parseQueryString[T](using rw: QueryStringRW[T]): T =
    val obj = parse(rawQueryString)
    rw.parse("", obj)
}

extension [T](value: T)(using rw: QueryStringRW[T]) {
  def toQueryString: String =
    rw.write("", value)
}

/* exceptions */
sealed class QuersonException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

final class ParsingException(val errors: Seq[ParseError])
    extends QuersonException(
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
