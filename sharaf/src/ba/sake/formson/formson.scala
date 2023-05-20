package ba.sake.formson

import scala.collection.mutable
import scala.deriving.*
import scala.compiletime.*
import java.util.UUID
import scala.util.Try
import ba.sake.validation.*
import java.nio.file.Path
import scala.collection.SortedMap

trait FormRW[T] {
  def read(path: String, formData: FormData): T
}

object FormRW {

  def apply[T](using instance: FormRW[T]): FormRW[T] = instance

  private def typeError(path: String, tpe: String, value: Any): Nothing =
    throw FieldsValidationException(
      Seq(
        FieldValidationError(path, value, s"invalid $tpe")
      )
    )

  private def typeMismatch(path: String, expectedTpe: String, tpe: String): Nothing =
    throw FormsonException(path, s"expected $expectedTpe but got $tpe")

  given FormRW[String] = new {
    def read(path: String, formData: FormData): String = formData match
      case FormData.Simple(formValue) =>
        formValue match
          case FormValue.Str(value) => value
          case FormValue.File(_)    => throw new RuntimeException("expected simple value but got a file")

      case other => typeMismatch(path, "String", other.tpe)
  }

  given FormRW[Int] = new {
    def read(path: String, formData: FormData): Int =
      val valueStr = FormRW[String].read(path, formData)
      valueStr.toIntOption.getOrElse(typeError(path, "Int", valueStr))
  }

  given FormRW[UUID] = new {
    def read(path: String, formData: FormData): UUID =
      val valueStr = FormRW[String].read(path, formData)
      Try(UUID.fromString(valueStr)).getOrElse(typeError(path, "UUID", valueStr))
  }

  given FormRW[Path] = new {
    def read(path: String, formData: FormData): Path = formData match
      case FormData.Simple(formValue) =>
        formValue match
          case FormValue.File(value) => value
          case FormValue.Str(value)  => throw new RuntimeException("expected file but got a simple value")

      case other => typeMismatch(path, "String", other.tpe)
  }

  given [T](using rw: FormRW[T]): FormRW[List[T]] = new {
    def read(path: String, formData: FormData): List[T] = formData match
      case FormData.Sequence(values) =>
        values.map(v => rw.read(path, v)).toList
      case other =>
        typeMismatch(path, "List", other.tpe)
  }

  inline given derived[T](using m: Mirror.Of[T]): FormRW[T] =
    inline m match
      case s: Mirror.SumOf[T]     => error("Sum types are not supported")
      case p: Mirror.ProductOf[T] => deriveProduct(p)

  private inline def deriveProduct[T](
      p: Mirror.ProductOf[T]
  ): FormRW[T] =
    val labels = constValueTuple[p.MirroredElemLabels].toArray.map(_.toString)
    val rws = getRWs[p.MirroredElemTypes].toArray
    new FormRW[T]:
      def read(path: String, formData: FormData): T = {

        val resTuple = labels.zip(rws).map { case (label, r) =>
          val subData = formData.asInstanceOf[FormData.Obj].values(label)
          val rw = r.asInstanceOf[FormRW[_]]
          rw.read(s"$path.$label", subData)
        }

        val tuple = Tuple.fromArray(resTuple)
        p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes])
      }

  // TODO a bit nicer recursive get-all-stuff
  // https://github.com/lampepfl/dotty/blob/3.2.2/tests/pos-special/fatal-warnings/not-looping-implicit.scala#L12

  private inline def getRWs[T <: Tuple]: Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts)  => summonInline[FormRW[`t`]] *: getRWs[ts]
}

////////////

class FormsonException(
    val path: String,
    val error: String,
    val rawValues: Map[String, Seq[FormValue]] = Map.empty
) extends Exception(s"Key '$path' error: $error")
