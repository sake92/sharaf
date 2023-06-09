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

def parseQueryString(rawQueryString: RawQueryString): QueryStringData.Obj = {
  val parser = new QuersonParser(rawQueryString)
  val qsInternal = parser.parse()
  fromInternal(qsInternal).asInstanceOf[QueryStringData.Obj]
}

private def fromInternal(fdi: QueryStringInternal): QueryStringData = fdi match
  case QueryStringInternal.Simple(value)       => QueryStringData.Simple(value)
  case QueryStringInternal.Obj(values)         => QueryStringData.Obj(values.view.mapValues(fromInternal).toMap)
  case QueryStringInternal.Sequence(valuesMap) => QueryStringData.Sequence(valuesMap.values.toSeq.flatten.map(fromInternal))

////////////////// INTERNAL parsing..
private[querson] class QuersonParser(rawQueryString: RawQueryString) {

  def parse(): QueryStringInternal.Obj = {

    // for every key we get an AST (object) with possibly recursive values
    val objects = rawQueryString.map { case (key, values) =>
      parseInternal(key, values)
    }.toSeq

    // then we merge all of them to one object
    mergeObjects(objects)
  }

  private def merge(acc: QueryStringInternal, second: QueryStringInternal): QueryStringInternal = (acc, second) match {

    case (QueryStringInternal.Simple(_), QueryStringInternal.Simple(_)) =>
      // we keep the first value
      // throw exc ?
      acc

    case (QueryStringInternal.Obj(existingValuesMap), QueryStringInternal.Obj(valuesMap)) =>
      val objAcc = existingValuesMap.to(mutable.SortedMap)
      valuesMap.foreach { case (key, value) =>
        objAcc.get(key) match
          case None =>
            objAcc(key) = value
          case Some(existingValue) =>
            objAcc(key) = merge(existingValue, value)
      }
      QueryStringInternal.Obj(objAcc.toMap)

    case (QueryStringInternal.Sequence(existingValuesMap), QueryStringInternal.Sequence(valuesMap)) =>
      val seqAcc = existingValuesMap.to(mutable.SortedMap)
      valuesMap.foreach { case (idx, values) =>
        seqAcc.get(idx) match
          case None => seqAcc(idx) = values
          case Some(existingValues) =>
            seqAcc(idx) = existingValues ++ values
      }
      QueryStringInternal.Sequence(seqAcc.to(SortedMap))

    case (first, second) =>
      throw new FormsonParsingException(s"Unmergeable objects: ${first.tpe} and ${second.tpe} ")
  }

  private def mergeObjects(flatObjects: Seq[QueryStringInternal.Obj]): QueryStringInternal.Obj = {
    flatObjects
      .foldLeft(QueryStringInternal.Obj(Map.empty)) { case (acc, next) =>
        merge(acc, next)
      }
      .asInstanceOf[QueryStringInternal.Obj]
  }

  private def parseInternal(rawKey: String, values: Seq[String]): QueryStringInternal.Obj = {

    // TODO ne mora bit index, morebit i key name !!
    val idxRegex = "(.*)\\[(.*)\\]".r

    split(rawKey) match {
      case (key, None) =>
        key match
          case idxRegex(k, idxstr) =>
            val valuesAsData = values.map(QueryStringInternal.Simple.apply)
            val idx = idxstr.toIntOption.getOrElse(0)
            val fd = QueryStringInternal.Sequence(SortedMap(idx -> valuesAsData))
            val fdValues = Map(k -> fd)
            QueryStringInternal.Obj(fdValues)
          case _ =>
            val fdValues = values.headOption.map(v => key -> QueryStringInternal.Simple(v)).toMap 
            QueryStringInternal.Obj(fdValues)
      case (key, Some(remainingRawKey)) =>
        key match
          case idxRegex(k, idxstr) =>
            val valuesAsData = parseInternal(remainingRawKey, values)
            val idx = idxstr.toIntOption.getOrElse(0)
            val fd = QueryStringInternal.Sequence(SortedMap(idx -> Seq(valuesAsData)))
            val fdValues = Map(k -> fd)
            QueryStringInternal.Obj(fdValues)
          case _ =>
            val fdValues = Map(key -> parseInternal(remainingRawKey, values))
            QueryStringInternal.Obj(fdValues)
    }
  }

  private def split(key: String): (String, Option[String]) = {
    val indexOfDot = key.indexOf('.')
    if indexOfDot == -1 then key -> None
    else {
      val (singleKey, rest) = key.splitAt(indexOfDot)
      // TODO if singleKey.isBlank then throw FormsonException(key, "Key is empty")
      singleKey -> Option(rest.dropWhile(_ == '.')).map(_.trim).filterNot(_.isBlank)
    }
  }

}

class FormsonParsingException(
    val msg: String
) extends Exception(msg)
