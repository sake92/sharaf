package ba.sake.sharaf

import scala.deriving.*
import scala.compiletime.*
import java.util.UUID
import scala.util.Try

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
  def extract(values: Option[Seq[String]]): Option[T]
}

object FromQueryStringParam {

  private def error(tpe: String, value: Any): Nothing =
    throw ValidationException(s"Invalid $tpe: '$value'")

  given FromQueryStringParam[String] = _.flatMap(_.headOption)
  given FromQueryStringParam[Int] =
    _.flatMap(_.headOption.map(v => v.toIntOption.getOrElse(error("Int", v))))
  given FromQueryStringParam[UUID] =
    _.flatMap(_.headOption.map(uuidStr => Try(UUID.fromString(uuidStr)).toOption.getOrElse(error("UUID", uuidStr))))

  given [T](using
      fqsp: FromQueryStringParam[T]
  ): FromQueryStringParam[Seq[T]] =
    valuesOpt =>
      val values = valuesOpt.getOrElse(Seq.empty)
      Some(values.flatMap(v => fqsp.extract(Some(Seq(v)))))

  given [T](using
      fqsp: FromQueryStringParam[T]
  ): FromQueryStringParam[Option[T]] =
    valuesOpt =>
      val values = valuesOpt.getOrElse(Seq.empty)
      Some(fqsp.extract(valuesOpt))

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

        val resTupleArray = labels.zip(reads).map { case (label, fqsp) =>
          val valuesOpt = qParams.get(label)
          fqsp.extract(valuesOpt)
        }

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
