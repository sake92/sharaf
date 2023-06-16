package ba.sake.querson

type RawQueryString = Map[String, Seq[String]]

enum QueryStringData(val tpe: String):

  case Simple(value: String) extends QueryStringData("simple value")

  case Sequence(values: Seq[QueryStringData]) extends QueryStringData("sequence")

  case Obj(values: Map[String, QueryStringData]) extends QueryStringData("object")
