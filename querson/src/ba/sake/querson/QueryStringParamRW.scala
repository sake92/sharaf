package ba.sake.querson

import java.util.UUID
import scala.util.Try
import ba.sake.validation.*

/** Write/read T from/to a sequence of query param values
  */
trait QueryStringParamRW[T] {

  def write(path: String, value: T): String

  def parse(path: String, values: Seq[String]): T

  /** Global default for `T` when key is missing.
    */
  def default: Option[T] = None

}

// TODO derive simple enums :)
object QueryStringParamRW {

  def apply[T](using instance: QueryStringParamRW[T]): QueryStringParamRW[T] = instance

  given QueryStringParamRW[String] with {

    override def write(path: String, value: String): String = s"$path=$value"

    override def parse(path: String, values: Seq[String]): String =
      values.headOption.getOrElse(parseError(path, "missing"))
  }

  given QueryStringParamRW[Int] with {
    override def write(path: String, value: Int): String = s"$path=$value"

    override def parse(path: String, values: Seq[String]): Int =
      println("Parsing int " + values)
      val str = QueryStringParamRW[String].parse(path, values)
      println(str.toIntOption)
      str.toIntOption.getOrElse(typeError(path, "Int", str))
  }

  given QueryStringParamRW[UUID] with {
    override def write(path: String, value: UUID): String = s"$path=$value"

    override def parse(path: String, values: Seq[String]): UUID =
      val str = QueryStringParamRW[String].parse(path, values)
      Try(UUID.fromString(str)).toOption.getOrElse(typeError(path, "UUID", str))
  }

  given [T](using fqsp: QueryStringParamRW[T]): QueryStringParamRW[Seq[T]] with {
    override def write(path: String, value: Seq[T]): String =
      value.map(v => fqsp.write(path, v)).mkString("&")

    // TODO try catch all items, rethrow
    override def parse(path: String, values: Seq[String]): Seq[T] =
      values.map(v => fqsp.parse(path, Seq(v)))

    override def default: Option[Seq[T]] = Some(Seq.empty)
  }

  given [T](using
      fqsp: QueryStringParamRW[T]
  ): QueryStringParamRW[Set[T]] with {
    override def write(path: String, value: Set[T]): String =
      QueryStringParamRW[Seq[T]].write(path, value.toSeq)

    override def parse(path: String, values: Seq[String]): Set[T] =
      QueryStringParamRW[Seq[T]].parse(path, values).toSet
    
    override def default: Option[Set[T]] = Some(Set.empty)
  }

  given [T](using
      fqsp: QueryStringParamRW[T]
  ): QueryStringParamRW[Option[T]] with {
    override def write(path: String, value: Option[T]): String =
      QueryStringParamRW[Seq[T]].write(path, value.toSeq)

    override def parse(path: String, values: Seq[String]): Option[T] =
      QueryStringParamRW[Seq[T]].parse(path, values).headOption

    override def default: Option[Option[T]] = Some(None)
  }

  private def typeError(name: String, tpe: String, value: Any): Nothing =
    throw FieldsValidationException(
      Seq(FieldValidationError(name, value, s"invalid $tpe"))
    )

  private def parseError(name: String, msg: String): Nothing =
    throw ParsingException(ParseError(name, msg))
}
