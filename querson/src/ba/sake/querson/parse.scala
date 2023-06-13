package ba.sake.querson

import scala.collection.mutable
import scala.collection.immutable.SortedMap

/** Takes a raw map of query string and converts it into a JSON-like AST
  *
  * @param rawQueryString
  *   Raw map of query string
  * @return
  *   Query string AST
  */

def parse(rawQueryString: RawQueryString): QueryStringData.Obj = {
  val parser = new QuersonParser(rawQueryString)
  val qsInternal = parser.parse()

  fromInternal(qsInternal).asInstanceOf[QueryStringData.Obj]
}

private def fromInternal(fdi: QueryStringInternal): QueryStringData = fdi match
  case QueryStringInternal.Simple(value) => QueryStringData.Simple(value)
  case QueryStringInternal.Obj(values)   => QueryStringData.Obj(values.view.mapValues(fromInternal).toMap)
  case QueryStringInternal.Sequence(valuesMap) =>
    QueryStringData.Sequence(valuesMap.values.toSeq.flatten.map(fromInternal))

////////////////// INTERNAL parsing..
private[querson] class QuersonParser(rawQueryString: RawQueryString) {
  import QueryStringInternal.*

  def parse(): Obj = {

    // for every key we get an AST (object) with possibly recursive values
    val objects = rawQueryString.map { case (key, values) =>
      parseInternal(key, values)
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
      throw new FormsonParsingException(s"Unmergeable objects: ${first.tpe} and ${second.tpe} ")
  }

  private def mergeObjects(flatObjects: Seq[Obj]): Obj = {
    flatObjects
      .foldLeft(Obj(Map.empty)) { case (acc, next) =>
        merge(acc, next)
      }
      .asInstanceOf[Obj]
  }

  private def parseInternal(rawKey: String, values: Seq[String]): Obj = {

    val seqRegex = "(.+)\\[([0-9]+)\\](.*)".r // abc[123]
    val bracketsKeyRegex = "(.+)\\[(.+)\\](.*)".r // abc[def]
    val emptySeqRegex = "(.+)\\[\\](.*)".r // abc[]
    val dotKeyRegex = "(.+)\\.(.+)".r // abc.def

    rawKey match
      case seqRegex(key, idx, rest) =>
        val internalValues =
          if rest.isBlank then values.map(Simple.apply)
          else Seq(parseInternal(rest, values))
        val index = idx.toInt
        val seq = Sequence(SortedMap(index -> internalValues))
        Obj(Map(key -> seq))

      case bracketsKeyRegex(key, subKey, subRest) =>
        val rest = if subRest.isBlank then subKey else s"$subKey.$subRest"
        val internalValues = parseInternal(rest, values)
        Obj(Map(key -> internalValues))

      case emptySeqRegex(key, rest) =>
        val internalValues =
          if rest.isBlank then values.map(Simple.apply)
          else Seq(parseInternal(rest, values))
        val seq = Sequence(SortedMap(0 -> internalValues))
        Obj(Map(key -> seq))

      case dotKeyRegex(key, rest) =>
        val internalValues = parseInternal(rest, values)
        Obj(Map(key -> internalValues))

      case key =>
        val valuesAsData = values.map(Simple.apply)
        val seq = Sequence(SortedMap(0 -> valuesAsData))
        Obj(Map(key -> seq))
  }

}

class FormsonParsingException(
    val msg: String
) extends Exception(msg)
