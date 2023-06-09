package ba.sake.querson

import scala.reflect.ClassTag
import scala.collection.mutable.ArrayDeque

import scala.deriving.*
import scala.quoted.*

import ba.sake.validation.*

// binds a case class from query params
// TODO
// - accept DEFAULT VALUE!
// - derive simple enums
trait QueryStringRW[T] {

  // TODO QueryStringData.Obj ??
  def write(path: String, value: T): String = ???

  def parse(path: String, qParams: QueryStringData.Obj): T
}

object QueryStringRW {

  def apply[T](using instance: QueryStringRW[T]): QueryStringRW[T] = instance

  /* macro derived instances */
  inline def derived[T]: QueryStringRW[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[QueryStringRW[T]] = {
    import quotes.reflect.*

    val mirror: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].getOrElse {
      report.errorAndAbort(
        s"Cannot derive QueryStringRW[${Type.show[T]}] automatically because it is not a case class"
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
            override def write(path: String, value: T): String = {
              val queryParams = scala.collection.mutable.ArrayDeque.empty[String]
              val valueAsProd = ${ 'value.asExprOf[Product] }
              $labels.zip(valueAsProd.productIterator).zip($rwInstances).foreach {
                case ((k, v), rw: QueryStringParamRW[?]) =>
                  val pathh = if path.isBlank then k else s"$path.$k"
                  queryParams += rw.asInstanceOf[QueryStringParamRW[Any]].write(pathh, v)
                case ((k, v), rw: QueryStringRW[?]) =>
                  queryParams += rw.asInstanceOf[QueryStringRW[Any]].write(k, v)
              }
              queryParams.mkString("&")
            }

            override def parse(path: String, qParams: QueryStringData.Obj): T = {
              val qParamsMap = qParams.values

              val arguments = ArrayDeque.empty[Any]
              val keyErrors = ArrayDeque.empty[ParseError]
              val keyValidationErrors = ArrayDeque.empty[FieldValidationError]
              val defaultValuesMap = $defaultValues.toMap

              $labels.zip($rwInstances).foreach { case (label, rw) =>
                val keyPath = if path.isBlank then label else s"$path.$label"
                val keyPresent = qParamsMap.contains(label)

                val globalDefault = Option
                  .when(rw.isInstanceOf[QueryStringParamRW[?]])(rw.asInstanceOf[QueryStringParamRW[?]].default)
                  .flatten
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
                        val res = rw match
                          case rw1: QueryStringParamRW[?] =>
                            qsData match
                              case QueryStringData.Simple(value) => rw1.parse(keyPath, Seq(value))
                              case QueryStringData.Sequence(values) =>
                                rw1.parse(keyPath, values.map(_.asInstanceOf[QueryStringData.Simple].value))
                              case other =>
                                throw ParsingException(ParseError(keyPath, s"Expected simple values but got $other"))
                          case rw2: QueryStringRW[?] =>
                            qsData match
                              case QueryStringData.Obj(values) =>
                                rw2.parse(keyPath, qsData.asInstanceOf[QueryStringData.Obj])
                              case other =>
                                throw ParsingException(ParseError(keyPath, s"Expected object but got $other"))

                        Some(res)
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

      case hmm => report.errorAndAbort(s"Sum types not supported ")
  }

  /* macro utils */
  private def summonInstances[Elems: Type](using Quotes): List[Expr[QueryStringParamRW[?] | QueryStringRW[?]]] =
    Type.of[Elems] match
      case '[elem *: elems] => summonInstance[elem] :: summonInstances[elems]
      case '[EmptyTuple]    => Nil

  private def summonInstance[Elem: Type](using Quotes): Expr[QueryStringParamRW[Elem] | QueryStringRW[Elem]] =
    import quotes.reflect.*
    Expr.summon[QueryStringParamRW[Elem]].getOrElse {
      Expr.summon[QueryStringRW[Elem]].getOrElse {
        report.errorAndAbort(
          s"There is no instance of QueryStringRW[${Type.show[Elem]}] available"
        )
      }
    }

  private def isSingletonCasesEnum[T: Type](using Quotes): Expr[Boolean] =
    import quotes.reflect.*
    val ts = TypeRepr.of[T].typeSymbol
    Expr(ts.flags.is(Flags.Enum) && ts.companionClass.methodMember("values").nonEmpty)

  private def defaultValuesExpr[T: Type](using
      Quotes
  ): Expr[List[(String, Option[() => Any])]] =
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

}
