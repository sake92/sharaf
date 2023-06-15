package ba.sake.querson

import java.util.UUID
import scala.util.Try
import ba.sake.validation.*
import scala.deriving.*
import scala.quoted.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Maps a `T` to/from query param values
  */
trait QueryStringParamRW[T] {

  def write(path: String, value: T): String

  def parse(path: String, values: Seq[String]): T

  /** Global default for `T` when key is missing.
    */
  def default: Option[T] = None

}

// TODO encode strings properly
object QueryStringParamRW {

  def apply[T](using instance: QueryStringParamRW[T]): QueryStringParamRW[T] = instance

  given QueryStringParamRW[String] with {

    override def write(path: String, value: String): String =
      val urlEncodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
      s"$path=$urlEncodedValue"

    override def parse(path: String, values: Seq[String]): String =
      values.headOption.getOrElse(parseError(path, "missing"))
  }

  given QueryStringParamRW[Int] with {
    override def write(path: String, value: Int): String =
      QueryStringParamRW[String].write(path, value.toString)

    override def parse(path: String, values: Seq[String]): Int =
      val str = QueryStringParamRW[String].parse(path, values)
      str.toIntOption.getOrElse(typeError(path, "Int", str))
  }

  given QueryStringParamRW[Long] with {
    override def write(path: String, value: Long): String =
      QueryStringParamRW[String].write(path, value.toString)

    override def parse(path: String, values: Seq[String]): Long =
      val str = QueryStringParamRW[String].parse(path, values)
      str.toIntOption.getOrElse(typeError(path, "Long", str))
  }

  given QueryStringParamRW[Double] with {
    override def write(path: String, value: Double): String =
      QueryStringParamRW[String].write(path, value.toString)

    override def parse(path: String, values: Seq[String]): Double =
      val str = QueryStringParamRW[String].parse(path, values)
      str.toDoubleOption.getOrElse(typeError(path, "Double", str))
  }

  given QueryStringParamRW[UUID] with {
    override def write(path: String, value: UUID): String =
      QueryStringParamRW[String].write(path, value.toString)

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

  given [T](using fqsp: QueryStringParamRW[T]): QueryStringParamRW[Set[T]] with {
    override def write(path: String, value: Set[T]): String =
      QueryStringParamRW[Seq[T]].write(path, value.toSeq)

    override def parse(path: String, values: Seq[String]): Set[T] =
      QueryStringParamRW[Seq[T]].parse(path, values).toSet

    override def default: Option[Set[T]] = Some(Set.empty)
  }

  given [T](using fqsp: QueryStringParamRW[T]): QueryStringParamRW[Option[T]] with {
    override def write(path: String, value: Option[T]): String =
      QueryStringParamRW[Seq[T]].write(path, value.toSeq)

    override def parse(path: String, values: Seq[String]): Option[T] =
      QueryStringParamRW[Seq[T]].parse(path, values).headOption

    override def default: Option[Option[T]] = Some(None)
  }

  /* macro derived instances */
  inline def derived[T]: QueryStringParamRW[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[QueryStringParamRW[T]] = {
    import quotes.reflect.*

    val mirror: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].getOrElse {
      report.errorAndAbort(
        s"Cannot derive QueryStringParamRW[${Type.show[T]}] automatically because ${Type.show[T]} is not an ADT"
      )
    }

    mirror match
      case '{
            type label <: Tuple;
            $m: Mirror.SumOf[T] { type MirroredElemLabels = `label` }
          } =>
        val labels = Expr(Type.valueOfTuple[label].map(_.toList.map(_.toString)).getOrElse(List.empty))

        val isSingleCasesEnum = isSingletonCasesEnum[T]
        if !isSingleCasesEnum then
          report.errorAndAbort(
            s"Cannot derive QueryStringParamRW[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton-cases enum"
          )

        val companion = TypeRepr.of[T].typeSymbol.companionModule.termRef
        val valueOfSelect = Select.unique(Ident(companion), "valueOf").symbol
        '{
          new QueryStringParamRW[T] {
            override def write(path: String, value: T): String =
              val index = $m.ordinal(value)
              val label = $labels(index)
              QueryStringParamRW[String].write(path, label)

            override def parse(path: String, values: Seq[String]): T =
              ${
                val bla = '{ values.head }
                Block(Nil, Apply(Select(Ident(companion), valueOfSelect), List(bla.asTerm))).asExprOf[T]
              }
          }
        }

      case hmm => report.errorAndAbort(s"Product types not supported ")
  }

  /* macro utils */
  private def isSingletonCasesEnum[T: Type](using Quotes): Boolean =
    import quotes.reflect.*
    val ts = TypeRepr.of[T].typeSymbol
    ts.flags.is(Flags.Enum) && ts.companionClass.methodMember("values").nonEmpty

    /* utils */
  private def typeError(name: String, tpe: String, value: Any): Nothing =
    throw FieldsValidationException(
      Seq(FieldValidationError(name, value, s"invalid $tpe"))
    )

  private def parseError(name: String, msg: String): Nothing =
    throw ParsingException(ParseError(name, msg))
}
