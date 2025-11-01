package ba.sake.querson

import scala.quoted.*

// stolen from https://github.com/iRevive/union-derivation/blob/main/modules/core/src/main/scala/io/github/irevive/union/derivation/IsUnion.scala

@annotation.implicitNotFound("${A} is not a union type")
trait IsUnion[A]

object IsUnion {

  // the only instance for IsUnion used to avoid overhead
  val singleton: IsUnion[Any] = new IsUnion[Any] {}

  transparent inline given derived[A]: IsUnion[A] = ${ deriveImpl[A] }

  private def deriveImpl[A](using quotes: Quotes, t: Type[A]): Expr[IsUnion[A]] = {
    import quotes.reflect.*
    val tpe: TypeRepr = TypeRepr.of[A]
    tpe.dealias match {
      case o: OrType => '{ IsUnion.singleton.asInstanceOf[IsUnion[A]] }.asExprOf[IsUnion[A]]
      case other     => report.errorAndAbort(s"${tpe.show} is not a Union")
    }
  }

}
