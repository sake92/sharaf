package ba.sake.validson

import scala.deriving.*
import scala.quoted.*
import scala.math.Ordered.*

trait Validator[T] {

  def validate(value: T): Seq[ValidationError]

  def and[F](getter: T => sourcecode.Text[F], predicate: F => Boolean, msg: String): Validator[T] =
    validatorImpl(getter, predicate, msg)

  // numbers
  def min[F: Numeric](getter: T => sourcecode.Text[F], value: F): Validator[T] =
    validatorImpl(getter, _ >= value, s"must be >= $value")

  def max[F: Numeric](getter: T => sourcecode.Text[F], value: F): Validator[T] =
    validatorImpl(getter, _ <= value, s"must be <= $value")

  def between[F: Numeric](getter: T => sourcecode.Text[F], min: F, max: F): Validator[T] =
    validatorImpl(getter, x => x >= min && x <= max, s"must be between [$min, $max]")

  def negative[F: Numeric](getter: T => sourcecode.Text[F]): Validator[T] =
    validatorImpl(getter, _ < summon[Numeric[F]].zero, s"must be negative")

  def nonPositive[F: Numeric](getter: T => sourcecode.Text[F]): Validator[T] =
    validatorImpl(getter, _ <= summon[Numeric[F]].zero, s"must be nonpositive")

  def positive[F: Numeric](getter: T => sourcecode.Text[F]): Validator[T] =
    validatorImpl(getter, _ > summon[Numeric[F]].zero, s"must be positive")

  def nonNegative[F: Numeric](getter: T => sourcecode.Text[F]): Validator[T] =
    validatorImpl(getter, _ >= summon[Numeric[F]].zero, s"must be nonnegative")

  // strings
  def notBlank(getter: T => sourcecode.Text[String]): Validator[T] =
    validatorImpl(getter, !_.isBlank, "must not be blank")

  def minLength(getter: T => sourcecode.Text[String], value: Long): Validator[T] =
    validatorImpl(getter, _.length >= value, s"must be >= $value")

  def maxLength(getter: T => sourcecode.Text[String], value: Long): Validator[T] =
    validatorImpl(getter, _.length <= value, s"must be <= $value")

  def contains(getter: T => sourcecode.Text[String], value: String): Validator[T] =
    validatorImpl(getter, _.contains(value), s"must contain $value")

  def matches(getter: T => sourcecode.Text[String], value: String): Validator[T] =
    validatorImpl(getter, _.matches(value), s"must contain $value")

  // seqs
  def minItems(getter: T => sourcecode.Text[Iterable[?]], value: Int): Validator[T] =
    validatorImpl(getter, _.size >= value, s"must be >= $value")
  
  def maxItems(getter: T => sourcecode.Text[Iterable[?]], value: Int): Validator[T] =
    validatorImpl(getter, _.size <= value, s"must be <= $value")

  private def validatorImpl[F](getter: T => sourcecode.Text[F], predicate: F => Boolean, msg: String): Validator[T] =
    (value: T) => {
      val fieldText = getter(value)
      val fieldLabel = fieldText.source.split("\\.").last // bit hacky but worky
      validate(value) ++ Option.unless(predicate(fieldText.value))(
        ValidationError(s".${fieldLabel}", msg, fieldText.value)
      )
    }
}

object Validator extends LowPriValidators {

  given seqValidator[T](using validator: Validator[T]): Validator[Seq[T]] with {
    override def validate(values: Seq[T]): Seq[ValidationError] = {
      val subErrors = values.zipWithIndex.flatMap { case (v, i) =>
        validator.validate(v).map(_.withPathPrefix(s"[$i]"))
      }
      subErrors
    }
  }

  /* macro derived instances */
  inline def derived[T]: Validator[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[Validator[T]] = {
    import quotes.reflect.*

    // only summon ProductOf ??
    val mirror: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].getOrElse {
      report.errorAndAbort(
        s"Cannot derive QueryStringRW[${Type.show[T]}] automatically because ${Type.show[T]} is not an ADT"
      )
    }

    mirror match
      case '{
            type label <: Tuple;
            $m: Mirror.ProductOf[T] { type MirroredElemTypes = elementTypes; type MirroredElemLabels = `label` }
          } =>
        val validatorInstancesExprs = summonInstances[elementTypes]
        val validatorInstancesExpr = Expr.ofList(validatorInstancesExprs)
        val labels = Expr(Type.valueOfTuple[label].map(_.toList.map(_.toString)).getOrElse(List.empty))

        '{
          new Validator[T] {
            override def validate(value: T): Seq[ValidationError] = {
              val valueAsProd = ${ 'value.asExprOf[Product] }
              val res = $labels.zip(valueAsProd.productIterator).zip($validatorInstancesExpr).flatMap {
                case ((label, v), validator) =>
                  validator.asInstanceOf[Validator[Any]].validate(v).map(_.withPathPrefix(s".${label}"))
              }
              res
            }
          }
        }

      case hmm => report.errorAndAbort("Sum types are not supported")
  }

  /* macro utils */
  private def summonInstances[Elems: Type](using Quotes): List[Expr[Validator[?]]] =
    Type.of[Elems] match
      case '[elem *: elems] => Expr.summon[Validator[elem]].get :: summonInstances[elems]
      case '[EmptyTuple]    => Nil

}

trait LowPriValidators {
  // noop if there is no validator for T
  given dummyValidator[T]: Validator[T] with {
    override def validate(values: T): Seq[ValidationError] =
      Seq.empty
  }
}
