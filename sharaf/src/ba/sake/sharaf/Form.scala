package ba.sake.sharaf

import scala.deriving.*
import scala.compiletime.*
import java.util.UUID
import scala.util.Try
import ba.sake.validation.*

import io.undertow.server.handlers.form.FormParserFactory
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormData
import io.undertow.server.handlers.form.FormData.FormValue

class FormsonException(val path: String, val error: String) extends Exception(s"Key '$path' is $error")

// extract T from form data
trait FromFormValue[T] {
  def extract(name: String, values: Seq[FormValue]): T
}

object FromFormValue {

  def apply[T](using instance: FromFormValue[T]): FromFormValue[T] = instance

  private def error(name: String, tpe: String, value: Any): Nothing =
    throw FieldsValidationException(
      Seq(
        FieldValidationError(name, value, s"invalid $tpe")
      )
    )

  given FromFormValue[String] = new {
    def extract(name: String, values: Seq[FormValue]): String =
      val valueStrOpt = values.headOption.map(_.getValue)
      valueStrOpt.getOrElse(throw new FormsonException(name, "missing"))
  }

  given FromFormValue[Int] = new {
    def extract(name: String, values: Seq[FormValue]): Int =
      val valueStr = FromFormValue[String].extract(name, values)
      valueStr.toIntOption.getOrElse(error(name, "Int", valueStr))
  }

  given FromFormValue[UUID] = new {
    def extract(name: String, values: Seq[FormValue]): UUID =
      val valueStr = FromFormValue[String].extract(name, values)
      Try(UUID.fromString(valueStr)).getOrElse(error(name, "UUID", valueStr))
  }

  given FromFormValue[java.nio.file.Path] = new {
    def extract(name: String, values: Seq[FormValue]): java.nio.file.Path =
      val fileItemOpt = values.headOption.filter(_.isFileItem).map(_.getFileItem.getFile)
      fileItemOpt.getOrElse(throw new FormsonException(name, "missing or is not a file"))
  }

  given [T](using
      ffv: FromFormValue[T]
  ): FromFormValue[Seq[T]] = new {
    def extract(name: String, values: Seq[FormValue]): Seq[T] =
      values.zipWithIndex.map { (v, i) =>
        val itemPath = s"$name[$i]"
        val value = values(i)
        ffv.extract(itemPath, Seq(v))
      }
  }

  given [T](using
      ffv: FromFormValue[T]
  ): FromFormValue[List[T]] = new {
    def extract(name: String, values: Seq[FormValue]): List[T] =
      values.zipWithIndex.map { (v, i) =>
        val itemPath = s"$name[$i]"
        val value = values(i)
        ffv.extract(itemPath, Seq(v))
      }.toList
  }

  given [T](using
      fqsp: FromFormValue[T]
  ): FromFormValue[Option[T]] = new {
    def extract(name: String, values: Seq[FormValue]): Option[T] =
      Some(fqsp.extract(name, values.toSeq))
  }

}

////////
// binds a case class from query params
trait FromFormData[T] {
  def bind(formData: Map[String, Seq[FormValue]]): T
}

object FromFormData {

  inline given derived[T](using m: Mirror.Of[T]): FromFormData[T] =
    inline m match
      case s: Mirror.SumOf[T]     => error("Sum types are not supported")
      case p: Mirror.ProductOf[T] => deriveProduct(p)

  private inline def deriveProduct[T](
      p: Mirror.ProductOf[T]
  ): FromFormData[T] =
    val labels = constValueTuple[p.MirroredElemLabels].toArray.map(_.toString)
    val reads = summonFFVs[p.MirroredElemTypes].toArray
    new {
      def bind(formData: Map[String, Seq[FormValue]]): T = {

        var errors = List.empty[FieldValidationError]
        val resTupleArray = labels.zip(reads).map { case (label, fqsp) =>
          val valuesOpt = formData.get(label).getOrElse(throw new FormsonException(label, "missing"))
          try {
            fqsp.extract(label, valuesOpt)
          } catch {
            case e: FieldsValidationException =>
              errors = errors ++ e.errors
              null
          }
        }
        if errors.nonEmpty then throw FieldsValidationException(errors)

        val tuple = Tuple.fromArray(resTupleArray)
        p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes])
      }
    }

  inline def summonFFVs[T <: Tuple]: List[FromFormValue[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) =>
        summonInline[FromFormValue[t]] :: summonFFVs[ts]
}
