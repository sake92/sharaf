package ba.sake.formson

import java.net.*
import java.nio.file.Path
import java.time.*
import java.util.UUID
import scala.deriving.*
import scala.quoted.*
import scala.reflect.ClassTag
import scala.collection.immutable.SeqMap
import scala.collection.mutable
import scala.util.Try
import ba.sake.formson.FormData.*

/** Maps a `T` to/from form data map
  */
trait FormDataRW[T] {

  def write(path: String, value: T): FormData

  def parse(path: String, formData: FormData): T

  /** Global default for `T` when key is missing.
    */
  def default: Option[T] = None
}

object FormDataRW {

  def apply[T](using instance: FormDataRW[T]): FormDataRW[T] = instance

  given FormDataRW[String] with {
    override def write(path: String, value: String): FormData =
      Simple(FormValue.Str(value))

    override def parse(path: String, formData: FormData): String = formData match
      case Simple(FormValue.Str(value))                    => value
      case Sequence(Seq(Simple(FormValue.Str(value)), _*)) => value
      case Sequence(Seq())                                 => parseError(path, s"is missing")
      case other                                           => parseError(path, s"has invalid type: ${other.tpe}")
  }

  given FormDataRW[Int] with {
    override def write(path: String, value: Int): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): Int =
      val str = FormDataRW[String].parse(path, formData)
      str.toIntOption.getOrElse(typeError(path, "Int", str))
  }

  given FormDataRW[Double] with {
    override def write(path: String, value: Double): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): Double =
      val str = FormDataRW[String].parse(path, formData)
      str.toDoubleOption.getOrElse(typeError(path, "Double", str))
  }

  given FormDataRW[UUID] with {
    override def write(path: String, value: UUID): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): UUID =
      val str = FormDataRW[String].parse(path, formData)
      Try(UUID.fromString(str)).toOption.getOrElse(typeError(path, "UUID", str))
  }

  // java.net
  given FormDataRW[URI] with {
    override def write(path: String, value: URI): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): URI =
      val str = FormDataRW[String].parse(path, formData)
      Try(URI(str)).toOption.getOrElse(typeError(path, "URI", str))
  }

  given FormDataRW[URL] with {
    override def write(path: String, value: URL): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): URL =
      val str = FormDataRW[String].parse(path, formData)
      Try(URI(str).toURL).toOption.getOrElse(typeError(path, "URL", str))
  }

  // java.time
  given FormDataRW[Instant] with {
    override def write(path: String, value: Instant): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): Instant =
      val str = FormDataRW[String].parse(path, formData)
      Try(Instant.parse(str)).toOption.getOrElse(typeError(path, "Instant", str))
  }

  given FormDataRW[LocalDate] with {
    override def write(path: String, value: LocalDate): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): LocalDate =
      val str = FormDataRW[String].parse(path, formData)
      Try(LocalDate.parse(str)).toOption.getOrElse(typeError(path, "LocalDate", str))
  }

  given FormDataRW[LocalDateTime] with {
    override def write(path: String, value: LocalDateTime): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): LocalDateTime =
      val str = FormDataRW[String].parse(path, formData)
      Try(LocalDateTime.parse(str)).toOption.getOrElse(typeError(path, "LocalDateTime", str))
  }

  given FormDataRW[Duration] with {
    override def write(path: String, value: Duration): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): Duration =
      val str = FormDataRW[String].parse(path, formData)
      Try(Duration.parse(str)).toOption.getOrElse(typeError(path, "Duration", str))
  }

  given FormDataRW[Period] with {
    override def write(path: String, value: Period): FormData =
      FormDataRW[String].write(path, value.toString)

    override def parse(path: String, formData: FormData): Period =
      val str = FormDataRW[String].parse(path, formData)
      Try(Period.parse(str)).toOption.getOrElse(typeError(path, "Period", str))
  }

  given FormDataRW[Path] with {
    override def write(path: String, value: Path): FormData =
      Simple(FormValue.File(value))

    override def parse(path: String, formData: FormData): Path = formData match
      case Simple(FormValue.File(value))                    => value
      case Sequence(Seq(Simple(FormValue.File(value)), _*)) => value
      case Sequence(Seq())                                  => parseError(path, "is missing")
      case other                                            => parseError(path, s"has invalid type: ${other.tpe}")
  }

  given FormDataRW[Array[Byte]] with {
    override def write(path: String, value: Array[Byte]): FormData =
      Simple(FormValue.ByteArray(value))

    override def parse(path: String, formData: FormData): Array[Byte] = formData match
      case Simple(FormValue.ByteArray(value))                    => value
      case Sequence(Seq(Simple(FormValue.ByteArray(value)), _*)) => value
      case Sequence(Seq())                                       => parseError(path, "is missing")
      case other =>
        parseError(path, s"has invalid type: ${other.tpe}")
  }

  given [T](using rw: FormDataRW[T]): FormDataRW[Option[T]] with {
    override def write(path: String, value: Option[T]): FormData =
      FormDataRW[Seq[T]].write(path, value.toSeq)

    override def parse(path: String, formData: FormData): Option[T] =
      val firstNonEmptyIndex = FormDataRW[Seq[String]].parse(path, formData).indexWhere(_.nonEmpty)
      Option.when(firstNonEmptyIndex >= 0) {
        FormDataRW[Seq[T]].parse(path, formData)(firstNonEmptyIndex)
      }

    override def default: Option[Option[T]] = Some(None)
  }

  /* collections */
  given [T](using rw: FormDataRW[T]): FormDataRW[Seq[T]] with {
    override def write(path: String, values: Seq[T]): FormData =
      val data = values.map(v => rw.write(path, v))
      Sequence(data)

    override def parse(path: String, formData: FormData): Seq[T] = formData match
      case Sequence(values) => parseRethrowingErrors(path, values)
      case other            => typeMismatchError(path, "Seq", other, None)

    override def default: Option[Seq[T]] = Some(Seq.empty)
  }

  given [T](using rw: FormDataRW[T]): FormDataRW[List[T]] with {
    override def write(path: String, values: List[T]): FormData =
      val data = values.map(v => rw.write(path, v))
      Sequence(data)

    override def parse(path: String, formData: FormData): List[T] = formData match
      case Sequence(values) => parseRethrowingErrors(path, values).toList
      case other            => typeMismatchError(path, "List", other, None)

    override def default: Option[List[T]] = Some(List.empty)
  }

  given [T](using rw: FormDataRW[T]): FormDataRW[Set[T]] with {
    override def write(path: String, values: Set[T]): FormData =
      FormDataRW[Seq[T]].write(path, values.toSeq)

    override def parse(path: String, formData: FormData): Set[T] = formData match
      case Sequence(values) => parseRethrowingErrors(path, values).toSet
      case other            => typeMismatchError(path, "Set", other, None)

    override def default: Option[Set[T]] = Some(Set.empty)
  }

  private def parseRethrowingErrors[T](path: String, values: Seq[FormData])(using
      rw: FormDataRW[T]
  ): Seq[T] = {
    val parsedValues = mutable.ArrayDeque.empty[T]
    val keyErrors = mutable.ArrayDeque.empty[ParseError]
    values.zipWithIndex.foreach { case (v, i) =>
      val subPath = s"$path[$i]"
      try {
        parsedValues += rw.parse(subPath, v)
      } catch {
        case pe: ParsingException =>
          keyErrors ++= pe.errors
      }
    }
    if keyErrors.nonEmpty then throw ParsingException(keyErrors.toSeq)

    parsedValues.toSeq
  }

  /* macro derived instances */
  inline def derived[T]: FormDataRW[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[FormDataRW[T]] = {
    import quotes.reflect.*

    // only summon ProductOf ??
    val mirror: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].getOrElse {
      report.errorAndAbort(
        s"Cannot derive FormDataRW[${Type.show[T]}] automatically because ${Type.show[T]} is not an ADT"
      )
    }

    mirror match
      case '{
            type label <: Tuple;
            $m: Mirror.ProductOf[T] { type MirroredElemTypes = elementTypes; type MirroredElemLabels = `label` }
          } =>
        val rwInstancesExpr = summonInstances[elementTypes]
        val rwInstances = Expr.ofList(rwInstancesExpr)
        val labels = Expr(Type.valueOfTuple[label].map(_.toList.map(_.toString)).getOrElse(List.empty))
        val defaultValues = defaultValuesExpr[T]

        '{
          new FormDataRW[T] {
            override def write(path: String, value: T): FormData = {
              val formDataMap = mutable.LinkedHashMap.empty[String, FormData]
              val valueAsProd = ${ 'value.asExprOf[Product] }
              $labels.zip(valueAsProd.productIterator).zip($rwInstances).foreach { case ((k, v), rw) =>
                val res = rw.asInstanceOf[FormDataRW[Any]].write(k, v)
                formDataMap += (k -> res)
              }
              Obj(SeqMap.from(formDataMap))
            }

            override def parse(path: String, formData: FormData): T = {
              val qParamsMap =
                if formData.isInstanceOf[Obj] then formData.asInstanceOf[Obj].values
                else typeMismatchError(path, "Object", formData, None)

              val arguments = mutable.ArrayDeque.empty[Any]
              val keyErrors = mutable.ArrayDeque.empty[ParseError]
              val defaultValuesMap = $defaultValues.toMap

              $labels.zip($rwInstances).foreach { case (label, rw) =>
                val keyPath = if path.isBlank then label else s"$path.$label"
                val keyPresent = qParamsMap.contains(label)

                val globalDefault = rw.default
                val hasGlobalDefault = globalDefault.nonEmpty

                val defaultOpt = defaultValuesMap(label)
                val hasLocalDefault = defaultOpt.isDefined

                if !keyPresent && !hasGlobalDefault && !hasLocalDefault then
                  keyErrors += ParseError(keyPath, "is missing")
                else {
                  val argOpt = qParamsMap
                    .get(label)
                    .flatMap { formData =>
                      try {
                        Some(rw.parse(keyPath, formData))
                      } catch {
                        case pe: ParsingException =>
                          keyErrors ++= pe.errors
                          None
                      }
                    }

                  argOpt
                    .orElse(defaultOpt.map(_()))
                    .orElse(globalDefault)
                    .foreach { arg =>
                      arguments += arg
                    }
                }
              }

              if keyErrors.nonEmpty then throw ParsingException(keyErrors.toSeq)

              $m.fromProduct(Tuple.fromArray(arguments.toArray))
            }
          }
        }

      case '{
            type label <: Tuple;
            $m: Mirror.SumOf[T] { type MirroredElemLabels = `label` }
          } =>
        val labels = Expr(Type.valueOfTuple[label].map(_.toList.map(_.toString)).getOrElse(List.empty))

        val isSingleCasesEnum = isSingletonCasesEnum[T]
        if !isSingleCasesEnum then
          report.errorAndAbort(
            s"Cannot derive FormDataRW[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton-cases enum"
          )

        val companion = TypeRepr.of[T].typeSymbol.companionModule.termRef
        val valueOfSelect = Select.unique(Ident(companion), "valueOf").symbol
        '{
          new FormDataRW[T] {
            override def write(path: String, value: T): FormData =
              val index = $m.ordinal(value)
              val label = $labels(index)
              FormDataRW[String].write(path, label)

            override def parse(path: String, formData: FormData): T =
              ${
                val labelQuote = '{ FormDataRW[String].parse(path, formData) }
                val tryBlock =
                  Block(Nil, Apply(Select(Ident(companion), valueOfSelect), List(labelQuote.asTerm))).asExprOf[T]
                '{
                  try {
                    $tryBlock
                  } catch {
                    case e: IllegalArgumentException =>
                      throw ParsingException(
                        ParseError(
                          path,
                          s"Enum value not found: '${$labelQuote}'. Possible values: ${$labels.map(l => s"'$l'").mkString(", ")}",
                          Some($labelQuote)
                        )
                      )
                  }
                }

              }
          }
        }

      case hmm => report.errorAndAbort(s"Sum types are not supported ")
  }

  /* macro utils */
  private def summonInstances[Elems: Type](using Quotes): List[Expr[FormDataRW[?]]] =
    Type.of[Elems] match
      case '[elem *: elems] => summonInstance[elem] :: summonInstances[elems]
      case '[EmptyTuple]    => Nil

  private def summonInstance[Elem: Type](using Quotes): Expr[FormDataRW[Elem]] =
    import quotes.reflect.*
    Expr.summon[FormDataRW[Elem]].getOrElse {
      report.errorAndAbort(
        s"There is no instance of FormDataRW[${Type.show[Elem]}] available"
      )
    }

  private def isSingletonCasesEnum[T: Type](using Quotes): Boolean =
    import quotes.reflect.*
    val ts = TypeRepr.of[T].typeSymbol
    ts.flags.is(Flags.Enum) && ts.companionClass.methodMember("values").nonEmpty

  private def defaultValuesExpr[T: Type](using Quotes): Expr[List[(String, Option[() => Any])]] =
    import quotes.reflect.*
    def exprOfOption(
        oet: (Expr[String], Option[Expr[Any]])
    ): Expr[(String, Option[() => Any])] = oet match {
      case (label, None)     => Expr(label.valueOrAbort -> None)
      case (label, Some(et)) => '{ $label -> Some(() => $et) }
    }
    val tpe = TypeRepr.of[T].typeSymbol
    val terms = tpe.primaryConstructor.paramSymss.flatten
      .filter(_.isValDef)
      .zipWithIndex
      .map { case (field, i) =>
        exprOfOption {
          Expr(field.name) -> tpe.companionClass
            .declaredMethod(s"$$lessinit$$greater$$default$$${i + 1}")
            .headOption
            .flatMap(_.tree.asInstanceOf[DefDef].rhs)
            .map(_.asExprOf[Any])
        }
      }
    Expr.ofList(terms)

  /* utils */
  private def typeError(path: String, tpe: String, value: Any): Nothing =
    throw ParsingException(ParseError(path, s"invalid $tpe", Some(value)))

  private def typeMismatchError(
      path: String,
      expectedType: String,
      formData: FormData,
      value: Option[Any]
  ): Nothing =
    throw ParsingException(
      ParseError(path, s"should be ${expectedType} but it is ${formData.tpe}", value)
    )

  private def parseError(path: String, msg: String): Nothing =
    throw ParsingException(ParseError(path, msg))
}
