package ba.sake.querson

import scala.collection.SortedMap

type RawQueryString = Map[String, Seq[String]]

enum QueryStringData(val tpe: String):

  case Simple(value: String) extends QueryStringData("simple value")

  case Sequence(values: Seq[QueryStringData]) extends QueryStringData("sequence")

  case Obj(values: Map[String, QueryStringData]) extends QueryStringData("object")

// internal, temporary representation
private[querson] enum QueryStringInternal(val tpe: String):

  case Simple(value: String) extends QueryStringInternal("simple value")

  case Sequence(values: SortedMap[Int, Seq[QueryStringInternal]]) extends QueryStringInternal("sequence")

  case Obj(values: Map[String, QueryStringInternal]) extends QueryStringInternal("object")
