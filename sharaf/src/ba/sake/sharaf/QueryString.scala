package ba.sake.sharaf

import scala.deriving.*
import scala.compiletime.*
import java.util.UUID
import scala.util.Try
import ba.sake.validation.*

final class QueryString(
    val params: Map[String, Seq[String]]
) {
  override def toString(): String =
    val p = params
      .map((k, values) => values.map(v => s"${k}=${v}").mkString("&"))
      .mkString("&")
    s"QueryString($p)"
}

object QueryString {
  def apply(params: Map[String, Seq[String]]): QueryString =
    new QueryString(params)
  def apply(params: (String, Seq[String])*): QueryString =
    new QueryString(params.toMap)
}

/////
// extract T from a sequence of query param values
// For query params a=123&a=456 the values would be Seq("123","456")
trait FromQueryStringParam[T] {
  def extract(name: String, valuesOpt: Option[Seq[String]]): Option[T]
}

object FromQueryStringParam {

  private def error(name: String, tpe: String, value: Any): Nothing =
    throw FieldsValidationException(
      Seq(
        FieldValidationError(name, value, s"invalid $tpe")
      )
    )

  given FromQueryStringParam[String] = new {
    def extract(name: String, valuesOpt: Option[Seq[String]]): Option[String] =
      valuesOpt.flatMap(_.headOption)
  }

  given FromQueryStringParam[Int] = new {
    def extract(name: String, valuesOpt: Option[Seq[String]]): Option[Int] =
      valuesOpt.flatMap(_.headOption.map(v => v.toIntOption.getOrElse(error(name, "Int", v))))
  }

  given FromQueryStringParam[UUID] = new {
    def extract(name: String, valuesOpt: Option[Seq[String]]): Option[UUID] =
      valuesOpt.flatMap(
        _.headOption.map(uuidStr => Try(UUID.fromString(uuidStr)).toOption.getOrElse(error(name, "UUID", uuidStr)))
      )
  }

  given [T](using
      fqsp: FromQueryStringParam[T]
  ): FromQueryStringParam[Seq[T]] = new {
    def extract(name: String, valuesOpt: Option[Seq[String]]): Option[Seq[T]] =
      val values = valuesOpt.getOrElse(Seq.empty)
      Some(values.flatMap(v => fqsp.extract(name, Some(Seq(v)))))
  }

  given [T](using
      fqsp: FromQueryStringParam[T]
  ): FromQueryStringParam[Set[T]] = new {
    def extract(name: String, valuesOpt: Option[Seq[String]]): Option[Set[T]] =
      val values = valuesOpt.getOrElse(Seq.empty)
      Some(values.flatMap(v => fqsp.extract(name, Some(Seq(v)))).toSet)
  }

  given [T](using
      fqsp: FromQueryStringParam[T]
  ): FromQueryStringParam[Option[T]] = new {
    def extract(name: String, valuesOpt: Option[Seq[String]]): Option[Option[T]] =
      val values = valuesOpt.getOrElse(Seq.empty)
      Some(fqsp.extract(name, valuesOpt))
  }

}

////////
// binds a case class from query params
trait FromQueryString[T] {
  def bind(qParams: Map[String, Seq[String]]): Option[T]
}

object FromQueryString {

  inline given derived[T](using m: Mirror.Of[T]): FromQueryString[T] =
    inline m match
      case s: Mirror.SumOf[T]     => error("Sum types are not supported")
      case p: Mirror.ProductOf[T] => deriveProduct(p)

  private inline def deriveProduct[T](
      p: Mirror.ProductOf[T]
  ): FromQueryString[T] =
    val labels = constValueTuple[p.MirroredElemLabels].toArray.map(_.toString)
    val reads = summonFQPs[p.MirroredElemTypes].toArray
    new {
      def bind(qParams: Map[String, Seq[String]]): Option[T] = {

        var errors = List.empty[FieldValidationError]
        val resTupleArray = labels.zip(reads).map { case (label, fqsp) =>
          val valuesOpt = qParams.get(label)
          try {
            fqsp.extract(label, valuesOpt)
          } catch {
            case e: FieldsValidationException =>
              errors = errors ++ e.errors
              null
          }
        }
        if errors.nonEmpty then throw FieldsValidationException(errors)

        if resTupleArray.exists(_.isEmpty) then None
        else
          val tuple = Tuple.fromArray(resTupleArray.flatten)
          Some(p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes]))
      }
    }

  inline def summonFQPs[T <: Tuple]: List[FromQueryStringParam[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) =>
        summonInline[FromQueryStringParam[t]] :: summonFQPs[ts]
}
