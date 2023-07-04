package ba.sake.validson

case class Rule[T](predicate: T => Boolean, msg: String)
