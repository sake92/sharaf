package ba.sake.querson

import scala.collection.SortedMap

type RawQueryString = Map[String, Seq[String]]

enum QueryStringData(val tpe: String) {
  case Simple(value: String) extends QueryStringData("simple value")
  case Obj(values: Map[String, QueryStringData]) extends QueryStringData("object")
  case Sequence(values: Seq[QueryStringData]) extends QueryStringData("sequence")
}

// internal, temporary representation
enum QueryStringInternal(val tpe: String) {
  case Simple(value: String) extends QueryStringInternal("simple value")
  case Obj(values: Map[String, QueryStringInternal]) extends QueryStringInternal("object")
  case Sequence(values: SortedMap[Int, Seq[QueryStringInternal]]) extends QueryStringInternal("sequence")
}
