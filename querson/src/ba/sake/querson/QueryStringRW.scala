package ba.sake.querson

import java.net.*
import java.time.*
import java.util.UUID

import scala.deriving.*
import scala.quoted.*
import scala.reflect.ClassTag
import scala.collection.mutable.ArrayDeque
import scala.util.Try

import QueryStringData.*

/** Maps a `T` to/from query params string map
  */
trait QueryStringRW[T] {

  def write(path: String, value: T): QueryStringData

  def parse(path: String, qsData: QueryStringData): T

  /** Global default for `T` when key is missing.
    */
  def default: Option[T] = None

  def bimap[U](f: U => T, g: T => U, default: Option[U] = None): QueryStringRW[U] =
    val self = this
    val _default = default
    new QueryStringRW[U] {
      override def parse(path: String, qsData: QueryStringData): U =
        g(self.parse(path, qsData))
      override def write(path: String, value: U): QueryStringData =
        self.write(path, f(value))
      override def default: Option[U] = _default
    }
}

object QueryStringRW {

  def apply[T](using instance: QueryStringRW[T]): QueryStringRW[T] = instance

  given QueryStringRW[String] with {
    override def write(path: String, value: String): QueryStringData =
      Simple(value)

    override def parse(path: String, qsData: QueryStringData): String = qsData match
      case Simple(value)                    => value
      case Sequence(Seq(Simple(value), _*)) => value
      case Sequence(Seq())                  => parseError(path, s"is missing")
      case other                            => parseError(path, s"has invalid type: ${other.tpe}")
  }

  given QueryStringRW[Boolean] with {
    override def write(path: String, value: Boolean): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): Boolean =
      val str = QueryStringRW[String].parse(path, qsData)
      str.toBooleanOption.getOrElse(typeError(path, "Boolean", str))
  }

  given QueryStringRW[Int] with {
    override def write(path: String, value: Int): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): Int =
      val str = QueryStringRW[String].parse(path, qsData)
      str.toIntOption.getOrElse(typeError(path, "Int", str))
  }

  given QueryStringRW[Long] with {
    override def write(path: String, value: Long): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): Long =
      val str = QueryStringRW[String].parse(path, qsData)
      str.toLongOption.getOrElse(typeError(path, "Long", str))
  }

  given QueryStringRW[Double] with {
    override def write(path: String, value: Double): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): Double =
      val str = QueryStringRW[String].parse(path, qsData)
      str.toDoubleOption.getOrElse(typeError(path, "Double", str))
  }

  given QueryStringRW[UUID] with {
    override def write(path: String, value: UUID): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): UUID =
      val str = QueryStringRW[String].parse(path, qsData)
      Try(UUID.fromString(str)).toOption.getOrElse(typeError(path, "UUID", str))
  }

  // java.net
  given QueryStringRW[URI] with {
    override def write(path: String, value: URI): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): URI =
      val str = QueryStringRW[String].parse(path, qsData)
      Try(URI(str)).toOption.getOrElse(typeError(path, "URI", str))
  }

  // java.time
  given QueryStringRW[Instant] with {
    override def write(path: String, value: Instant): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): Instant =
      val str = QueryStringRW[String].parse(path, qsData)
      Try(Instant.parse(str)).toOption.getOrElse(typeError(path, "Instant", str))
  }

  given QueryStringRW[LocalDate] with {
    override def write(path: String, value: LocalDate): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): LocalDate =
      val str = QueryStringRW[String].parse(path, qsData)
      Try(LocalDate.parse(str)).toOption.getOrElse(typeError(path, "LocalDate", str))
  }

  given QueryStringRW[LocalDateTime] with {
    override def write(path: String, value: LocalDateTime): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): LocalDateTime =
      val str = QueryStringRW[String].parse(path, qsData)
      Try(LocalDateTime.parse(str)).toOption.getOrElse(typeError(path, "LocalDateTime", str))
  }

  given QueryStringRW[Duration] with {
    override def write(path: String, value: Duration): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): Duration =
      val str = QueryStringRW[String].parse(path, qsData)
      Try(Duration.parse(str)).toOption.getOrElse(typeError(path, "Duration", str))
  }

  given QueryStringRW[Period] with {
    override def write(path: String, value: Period): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): Period =
      val str = QueryStringRW[String].parse(path, qsData)
      Try(Period.parse(str)).toOption.getOrElse(typeError(path, "Period", str))
  }

  /* collections */
  given [T](using fqsp: QueryStringRW[T]): QueryStringRW[Option[T]] with {
    override def write(path: String, value: Option[T]): QueryStringData =
      QueryStringRW[Seq[T]].write(path, value.toSeq)

    override def parse(path: String, qsData: QueryStringData): Option[T] =
      val firstNonEmptyIndex = QueryStringRW[Seq[String]].parse(path, qsData).indexWhere(_.nonEmpty)
      Option.when(firstNonEmptyIndex >= 0) {
        QueryStringRW[Seq[T]].parse(path, qsData)(firstNonEmptyIndex)
      }

    override def default: Option[Option[T]] = Some(None)
  }

  given [T](using rw: QueryStringRW[T]): QueryStringRW[Seq[T]] with {
    override def write(path: String, values: Seq[T]): QueryStringData =
      val data = values.map(v => rw.write(path, v))
      Sequence(data)

    override def parse(path: String, qsData: QueryStringData): Seq[T] = qsData match
      case Sequence(values) => parseRethrowingErrors(path, values)
      case other            => typeMismatchError(path, "Seq", other, None)

    override def default: Option[Seq[T]] = Some(Seq.empty)
  }

  given [T](using rw: QueryStringRW[T]): QueryStringRW[Set[T]] with {
    override def write(path: String, values: Set[T]): QueryStringData =
      QueryStringRW[Seq[T]].write(path, values.toSeq)

    override def parse(path: String, qsData: QueryStringData): Set[T] = qsData match
      case Sequence(values) => parseRethrowingErrors(path, values).toSet
      case other            => typeMismatchError(path, "Set", other, None)

    override def default: Option[Set[T]] = Some(Set.empty)
  }

  private def parseRethrowingErrors[T](path: String, values: Seq[QueryStringData])(using
      rw: QueryStringRW[T]
  ): Seq[T] = {
    val parsedValues = ArrayDeque.empty[T]
    val keyErrors = ArrayDeque.empty[ParseError]
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
  inline def derived[T]: QueryStringRW[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[QueryStringRW[T]] = {
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
        val rwInstancesExpr = summonInstances[elementTypes]
        val rwInstances = Expr.ofList(rwInstancesExpr)
        val labels = Expr(Type.valueOfTuple[label].map(_.toList.map(_.toString)).getOrElse(List.empty))
        val defaultValues = defaultValuesExpr[T]

        '{
          new QueryStringRW[T] {
            override def write(path: String, value: T): QueryStringData = {
              val queryStringMap = scala.collection.mutable.Map.empty[String, QueryStringData]
              val valueAsProd = ${ 'value.asExprOf[Product] }
              $labels.zip(valueAsProd.productIterator).zip($rwInstances).foreach { case ((k, v), rw) =>
                val res = rw.asInstanceOf[QueryStringRW[Any]].write(k, v)
                queryStringMap += (k -> res)
              }
              Obj(queryStringMap.toMap)
            }

            override def parse(path: String, qsData: QueryStringData): T = {
              val qParamsMap =
                if qsData.isInstanceOf[Obj] then qsData.asInstanceOf[Obj].values
                else typeMismatchError(path, "Object", qsData, None)

              val arguments = ArrayDeque.empty[Any]
              val keyErrors = ArrayDeque.empty[ParseError]
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
                    .flatMap { qsData =>
                      try {
                        Some(rw.parse(keyPath, qsData))
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
            s"Cannot derive QueryStringRW[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton-cases enum"
          )

        val companion = TypeRepr.of[T].typeSymbol.companionModule.termRef
        val valueOfSelect = Select.unique(Ident(companion), "valueOf").symbol
        '{
          new QueryStringRW[T] {
            override def write(path: String, value: T): QueryStringData =
              val index = $m.ordinal(value)
              val label = $labels(index)
              QueryStringRW[String].write(path, label)

            override def parse(path: String, qsData: QueryStringData): T =
              ${
                val labelQuote = '{ QueryStringRW[String].parse(path, qsData) }
                val tryBlock =
                  Block(Nil, Apply(Select(Ident(companion), valueOfSelect), List(labelQuote.asTerm))).asExprOf[T]
                '{
                  try {
                    $tryBlock
                  } catch {
                    case _: IllegalArgumentException =>
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

      case _ => report.errorAndAbort("Sum types are not supported")
  }

  /* macro utils */
  private def summonInstances[Elems: Type](using Quotes): List[Expr[QueryStringRW[?]]] =
    Type.of[Elems] match
      case '[elem *: elems] => summonInstance[elem] :: summonInstances[elems]
      case '[EmptyTuple]    => Nil

  private def summonInstance[Elem: Type](using Quotes): Expr[QueryStringRW[Elem]] =
    import quotes.reflect.*
    Expr.summon[QueryStringRW[Elem]].getOrElse {
      report.errorAndAbort(
        s"There is no instance of QueryStringRW[${Type.show[Elem]}] available"
      )
    }

  private def isSingletonCasesEnum[T: Type](using Quotes): Boolean =
    import quotes.reflect.*
    val ts = TypeRepr.of[T].typeSymbol
    ts.flags.is(Flags.Enum) && ts.companionClass.methodMember("values").nonEmpty

  // adapted from https://github.com/lampepfl/dotty-macro-examples/blob/main/defaultParamsInference/src/macro.scala
  // and magnolia
  private def defaultValuesExpr[T: Type](using Quotes): Expr[List[(String, Option[() => Any])]] =
    import quotes.reflect.*
    def exprOfOption(
        oet: (Expr[String], Option[Expr[Any]])
    ): Expr[(String, Option[() => Any])] = oet match {
      case (label, None)     => Expr(label.valueOrAbort -> None)
      case (label, Some(et)) => '{ $label -> Some(() => $et) }
    }
    val tpe = TypeTree.of[T].symbol
    val terms = tpe.caseFields.zipWithIndex
      .map { case (field, i) =>
        val res = exprOfOption {
          Expr(field.name) -> tpe.companionClass.tree
            .asInstanceOf[ClassDef]
            .body
            .collectFirst {
              case deff @ DefDef(name, _, _, _) if name == s"$$lessinit$$greater$$default$$${i + 1}" =>
                deff.rhs.map(_.asExprOf[Any])
            }
            .flatten
        }
        res
      }

    Expr.ofList(terms)

  /* utils */
  private def typeError(path: String, tpe: String, value: Any): Nothing =
    throw ParsingException(ParseError(path, s"invalid $tpe", Some(value)))

  private def typeMismatchError(
      path: String,
      expectedType: String,
      qsData: QueryStringData,
      value: Option[Any]
  ): Nothing =
    throw ParsingException(
      ParseError(path, s"should be ${expectedType} but it is ${qsData.tpe}", value)
    )

  private def parseError(path: String, msg: String): Nothing =
    throw ParsingException(ParseError(path, msg))
}
