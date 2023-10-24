package ba.sake.sharaf
package routing

import java.util.UUID
import scala.deriving.*
import scala.quoted.*
import scala.util.Try

// typeclass for converting a path parameter to T
trait FromPathParam[T] {
  def parse(str: String): Option[T]
}

object FromPathParam {
  given FromPathParam[Int] = new {
    def parse(str: String): Option[Int] = str.toIntOption
  }
  given FromPathParam[Long] = new {
    def parse(str: String): Option[Long] = str.toLongOption
  }
  given FromPathParam[UUID] = new {
    def parse(str: String): Option[UUID] = Try(UUID.fromString(str)).toOption
  }

  /* macro derivation */
  inline def derived[T]: FromPathParam[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[FromPathParam[T]] = {
    import quotes.reflect.*

    val mirror: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].getOrElse {
      report.errorAndAbort(
        s"Cannot derive FromPathParam[${Type.show[T]}] automatically because ${Type.show[T]} is not an ADT"
      )
    }

    mirror match
      case '{
            $m: Mirror.ProductOf[T]
          } =>
        report.errorAndAbort(
          s"Cannot derive FromPathParam[${Type.show[T]}] automatically because product types are not supported"
        )

      case '{
            type label <: Tuple;
            $m: Mirror.SumOf[T] { type MirroredElemLabels = `label` }
          } =>
        val isSingleCasesEnum = isSingletonCasesEnum[T]
        if !isSingleCasesEnum then
          report.errorAndAbort(
            s"Cannot derive FromPathParam[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton-cases enum"
          )

        val companion = TypeRepr.of[T].typeSymbol.companionModule.termRef
        val valueOfSelect = Select.unique(Ident(companion), "valueOf").symbol
        '{
          new FromPathParam[T] {
            override def parse(str: String): Option[T] =
              ${
                val labelQuote = 'str
                val tryBlock =
                  Block(Nil, Apply(Select(Ident(companion), valueOfSelect), List(labelQuote.asTerm))).asExprOf[T]
                '{
                  try {
                    Option($tryBlock)
                  } catch {
                    case e: IllegalArgumentException =>
                      None
                  }
                }
              }
          }
        }

      case hmm => report.errorAndAbort("Not supported")
  }

  private def isSingletonCasesEnum[T: Type](using Quotes): Boolean =
    import quotes.reflect.*
    val ts = TypeRepr.of[T].typeSymbol
    ts.flags.is(Flags.Enum) && ts.companionClass.methodMember("values").nonEmpty

}
