package ba.sake.querson

import scala.deriving.*
import scala.quoted.*
import scala.reflect.ClassTag
import scala.collection.mutable.ArrayDeque

import ba.sake.validation.*
import java.util.UUID

/** Maps a `T` to/from query params string
  */
trait QueryStringRW[T] {

  def write(path: String, value: T): QueryStringData

  def parse(path: String, qsData: QueryStringData): T

  /** Global default for `T` when key is missing.
    */
  def default: Option[T] = None
}

object QueryStringRW {

  def apply[T](using instance: QueryStringRW[T]): QueryStringRW[T] = instance

  given QueryStringRW[String] with {
    override def write(path: String, value: String): QueryStringData =
      QueryStringData.Simple(value)

    override def parse(path: String, qsData: QueryStringData): String = qsData match
      case QueryStringData.Simple(value) => value
      // TODO better handle case when multiple values
      case seq: QueryStringData.Sequence => seq.values.head.asInstanceOf[QueryStringData.Simple].value
      case other                         => parseError(path, "Unexpected QueryStringData: $other")
  }

  given QueryStringRW[Int] with {
    override def write(path: String, value: Int): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

      // TODO error handling..
    override def parse(path: String, qsData: QueryStringData): Int =
      val str = QueryStringRW[String].parse(path, qsData)
      str.toInt
  }

  given QueryStringRW[UUID] with {
    override def write(path: String, value: UUID): QueryStringData =
      QueryStringRW[String].write(path, value.toString)

    override def parse(path: String, qsData: QueryStringData): UUID =
      val str = QueryStringRW[String].parse(path, qsData)
      UUID.fromString(str)
  }

  given [T](using rw: QueryStringRW[T]): QueryStringRW[Seq[T]] with {
    override def write(path: String, values: Seq[T]): QueryStringData =
      val data = values.map(v => rw.write(path, v))
      QueryStringData.Sequence(data)

    // TODO try catch all items, rethrow
    override def parse(path: String, qsData: QueryStringData): Seq[T] = qsData match
      case QueryStringData.Sequence(values) => values.map(v => rw.parse(path, v))
      case other                            => typeError(path, "Seq", other)

    override def default: Option[Seq[T]] = Some(Seq.empty)
  }

  given [T](using fqsp: QueryStringRW[T]): QueryStringRW[Option[T]] with {
    override def write(path: String, value: Option[T]): QueryStringData =
      QueryStringRW[Seq[T]].write(path, value.toSeq)

    override def parse(path: String, qsData: QueryStringData): Option[T] =
      QueryStringRW[Seq[T]].parse(path, qsData).headOption

    override def default: Option[Option[T]] = Some(None)
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
              QueryStringData.Obj(queryStringMap.toMap)
            }

            override def parse(path: String, qsData: QueryStringData): T = {
              // TODO handle exc
              val qParamsMap = qsData.asInstanceOf[QueryStringData.Obj].values

              val arguments = ArrayDeque.empty[Any]
              val keyErrors = ArrayDeque.empty[ParseError]
              val keyValidationErrors = ArrayDeque.empty[FieldValidationError]
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
                        case e: FieldsValidationException =>
                          keyValidationErrors ++= e.errors
                          None
                      }
                    }

                  //  TODO macro generate validation typeclass...
                  argOpt
                    .orElse(defaultOpt.map(_()))
                    .orElse(globalDefault)
                    .foreach { arg =>
                      arguments += arg
                    }
                }
              }

              if keyErrors.nonEmpty then throw ParsingException(keyErrors.toSeq)
              if keyValidationErrors.nonEmpty then throw FieldsValidationException(keyValidationErrors.toSeq)

              try {
                // TODO validation..
                $m.fromProduct(Tuple.fromArray(arguments.toArray))
              } catch {
                case fve: FieldsValidationException =>
                  val validationErrors =
                    keyValidationErrors.toSeq ++ fve.errors.map(e => e.withPath(s"$path.${e.path}"))
                  throw new FieldsValidationException(validationErrors)
              }
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
                val valueQuote = '{ QueryStringRW[String].parse(path, qsData) }
                Block(Nil, Apply(Select(Ident(companion), valueOfSelect), List(valueQuote.asTerm))).asExprOf[T]
              }
          }
        }

      case hmm => report.errorAndAbort(s"Sum types are not supported ")
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

  private def defaultValuesExpr[T: Type](using Quotes): Expr[List[(String, Option[() => Any])]] =
    import quotes.reflect._
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
  private def typeError(name: String, tpe: String, value: Any): Nothing =
    throw FieldsValidationException(
      Seq(FieldValidationError(name, value, s"invalid $tpe"))
    )

  private def parseError(name: String, msg: String): Nothing =
    throw ParsingException(ParseError(name, msg))
}
