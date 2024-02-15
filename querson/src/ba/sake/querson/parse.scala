package ba.sake.querson

import scala.collection.mutable
import scala.collection.immutable.SortedMap
import fastparse.Parsed.Success
import fastparse.Parsed.Failure

/** Takes a raw map of query string and converts it into a JSON-like AST
  *
  * @param qsMap
  *   Raw map of query string
  * @return
  *   Query string AST
  */

def parseQSMap(queryStringMap: QueryStringMap): QueryStringData =
  val parser = QuersonParser(queryStringMap)
  val qsInternal = parser.parse()
  fromInternal(qsInternal)

private def fromInternal(qsi: QueryStringInternal): QueryStringData = qsi match
  case QueryStringInternal.Simple(value)       => QueryStringData.Simple(value)
  case QueryStringInternal.Obj(values)         => QueryStringData.Obj(values.view.mapValues(fromInternal).toMap)
  case QueryStringInternal.Sequence(valuesMap) =>
    // TODO doesnt work for List[List[T]]
    QueryStringData.Sequence(valuesMap.values.toSeq.flatten.map(fromInternal))

// internal, temporary representation
private[querson] enum QueryStringInternal(val tpe: String):
  case Simple(value: String) extends QueryStringInternal("simple value")
  case Sequence(values: SortedMap[Int, Seq[QueryStringInternal]]) extends QueryStringInternal("sequence")
  case Obj(values: Map[String, QueryStringInternal]) extends QueryStringInternal("object")

////////////////// INTERNAL parsing..
private[querson] class QuersonParser(qsMap: QueryStringMap) {
  import QueryStringInternal.*

  def parse(): Obj = {

    // for every key we get an AST (object) with possibly recursive values
    val objects = qsMap.map { case (key, values) =>
      val keyParts = KeyParser(key).parse()
      parseInternal(keyParts, values).asInstanceOf[Obj]
    }.toSeq

    // then we merge all of them to one object
    mergeObjects(objects)
  }

  private def merge(acc: QueryStringInternal, second: QueryStringInternal): QueryStringInternal = (acc, second) match {

    case (Simple(_), Simple(_)) =>
      // - if we get many values we juts merge them into a sequence
      // - this could happen when you have a=a1 & a[]=a2 for example
      // both should be considered as part of the same Seq..
      Sequence(SortedMap(0 -> Seq(acc, second)))

    case (Obj(existingValuesMap), Obj(valuesMap)) =>
      val objAcc = existingValuesMap.to(mutable.SortedMap)
      valuesMap.foreach { case (key, value) =>
        objAcc.get(key) match
          case None =>
            objAcc(key) = value
          case Some(existingValue) =>
            objAcc(key) = merge(existingValue, value)
      }
      Obj(objAcc.toMap)

    case (Sequence(existingValuesMap), Sequence(valuesMap)) =>
      val seqAcc = existingValuesMap.to(mutable.SortedMap)
      valuesMap.foreach { case (idx, values) =>
        seqAcc.get(idx) match
          case None => seqAcc(idx) = values
          case Some(existingValues) =>
            seqAcc(idx) = existingValues ++ values
      }
      Sequence(seqAcc.to(SortedMap))

    case (first, second) =>
      throw new QuersonException(s"Unmergeable objects: ${first.tpe} and ${second.tpe}")
  }

  private def mergeObjects(flatObjects: Seq[Obj]): Obj = {
    flatObjects
      .foldLeft(Obj(Map.empty)) { case (acc, next) =>
        merge(acc, next)
      }
      .asInstanceOf[Obj]
  }

  private def parseInternal(keyParts: Seq[String], values: Seq[String]): QueryStringInternal = {

    keyParts match
      case Seq(key, rest*) =>
        val adaptedKey = if key.isBlank then "0" else key
        adaptedKey.toIntOption match
          case Some(index) =>
            if rest.isEmpty
            then Sequence(SortedMap(index -> values.map(Simple.apply)))
            else Sequence(SortedMap(index -> Seq(parseInternal(rest, values))))

          case None =>
            if rest.isEmpty then Obj(Map(key -> Sequence(SortedMap(0 -> values.map(Simple.apply)))))
            else Obj(Map(key -> parseInternal(rest, values)))

      case Seq() => throw QuersonException("Empty key parts")
  }

}

private[querson] class KeyParser(key: String) {
  import fastparse.*, NoWhitespace.*

  private val ForbiddenKeyChars = Set('[', ']', '.')

  def parse(): Seq[String] = {

    val res = fastparse.parse(key, parseFinal(_))
    res match
      case Success((firstKey, subKeys), index) => subKeys.prepended(firstKey)
      case f: Failure                          => throw QuersonException(f.msg)
  }

  private def parseFinal[$: P] = P(
    Start ~ parseKey ~ (parseBracketedSubKey | parseDottedSubKey | parseIndex).rep(min = 0) ~ End
  )

  private def parseKey[$: P] = P(CharPred(c => !ForbiddenKeyChars(c) && !c.isWhitespace).rep(min = 1).!)

  private def parseBracketedSubKey[$: P] = P("[" ~ parseKey ~ "]")

  private def parseDottedSubKey[$: P] = P("." ~ parseKey)

  private def parseIndex[$: P] = P("[" ~ CharIn("0-9").rep(min = 0).! ~ "]")

}
