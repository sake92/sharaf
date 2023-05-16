package ba.sake.formson

import scala.collection.mutable
import scala.deriving.*
import scala.compiletime.*
import java.util.UUID
import scala.util.Try
import ba.sake.validation.*
import java.nio.file.Path
import scala.collection.SortedMap

trait FromFormData[T] {
  def bind(path: String, formData: FormData): T
}

object FromFormData {

  def apply[T](using instance: FromFormData[T]): FromFormData[T] = instance

  private def typeError(path: String, tpe: String, value: Any): Nothing =
    throw FieldsValidationException(
      Seq(
        FieldValidationError(path, value, s"invalid $tpe")
      )
    )

  private def typeMismatch(path: String, expectedTpe: String, tpe: String): Nothing =
    throw FormsonException(path, s"expected $expectedTpe but got $tpe")

  given FromFormData[String] = new {
    def bind(path: String, formData: FormData): String = formData match
      case FormData.Simple(formValue) =>
        formValue match
          case FormValue.Str(value) => value
          case FormValue.File(_)    => throw new RuntimeException("expected simple value but got a file")

      case other => typeMismatch(path, "String", other.tpe)
  }

  given FromFormData[Int] = new {
    def bind(path: String, formData: FormData): Int =
      val valueStr = FromFormData[String].bind(path, formData)
      valueStr.toIntOption.getOrElse(typeError(path, "Int", valueStr))
  }

  given FromFormData[UUID] = new {
    def bind(path: String, formData: FormData): UUID =
      val valueStr = FromFormData[String].bind(path, formData)
      Try(UUID.fromString(valueStr)).getOrElse(typeError(path, "UUID", valueStr))
  }

  given FromFormData[Path] = new {
    def bind(path: String, formData: FormData): Path = formData match
      case FormData.Simple(formValue) =>
        formValue match
          case FormValue.File(value) => value
          case FormValue.Str(value)  => throw new RuntimeException("expected file but got a simple value")

      case other => typeMismatch(path, "String", other.tpe)
  }

  given [T](using ffv: FromFormData[T]): FromFormData[List[T]] = new {
    def bind(path: String, formData: FormData): List[T] = formData match
      case FormData.Sequence(values) =>
        values.map(v => ffv.bind(path, v)).toList
      case other =>
        typeMismatch(path, "List", other.tpe)
  }

  inline given derived[T](using m: Mirror.Of[T]): FromFormData[T] =
    inline m match
      case s: Mirror.SumOf[T]     => error("Sum types are not supported")
      case p: Mirror.ProductOf[T] => deriveProduct(p)

  private inline def deriveProduct[T](
      p: Mirror.ProductOf[T]
  ): FromFormData[T] =
    val labels = constValueTuple[p.MirroredElemLabels].toArray.map(_.toString)
    val froms = getFroms[p.MirroredElemTypes].toArray
    new FromFormData[T]:
      def bind(path: String, formData: FormData): T = {

        val resTuple = labels.zip(froms).map { case (label, r) =>
          val subData = formData.asInstanceOf[FormData.Obj].values(label)
          val from = r.asInstanceOf[FromFormData[_]]
          from.bind(s"$path.$label", subData)
        }

        val tuple = Tuple.fromArray(resTuple)
        p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes])
      }

  // TODO a bit nicer recursive get-all-stuff
  // https://github.com/lampepfl/dotty/blob/3.2.2/tests/pos-special/fatal-warnings/not-looping-implicit.scala#L12

  private inline def getFroms[T <: Tuple]: Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts)  => summonInline[FromFormData[`t`]] *: getFroms[ts]
}

////////////

class FormsonException(
    val path: String,
    val error: String,
    val rawValues: Map[String, Seq[FormValue]] = Map.empty
) extends Exception(s"Key '$path' error: $error")
