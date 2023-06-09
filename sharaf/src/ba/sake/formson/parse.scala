package ba.sake.formson

import scala.collection.mutable
import scala.collection.immutable.SortedMap

/*
value is Seq[FormValue] because we can have multiple SAME keys:
my_values[]=a
my_values[]=b
 */
/** Takes a flattened map received from HTML form and converts it into a JSON-like AST
  *
  * @param rawMap
  *   Flat map of key-values in a form
  * @return
  *   Form AST
  */

def parseForm(rawMap: FlatFormValues): FormData = {
  val parser = new FormsonParser(rawMap)
  val formDataInternal = parser.parse()
  fromInternal(formDataInternal)
}

private def fromInternal(fdi: FormDataInternal): FormData = fdi match
  case FormDataInternal.Simple(value)       => FormData.Simple(value)
  case FormDataInternal.Obj(values)         => FormData.Obj(values.view.mapValues(fromInternal).toMap)
  case FormDataInternal.Sequence(valuesMap) => FormData.Sequence(valuesMap.values.toSeq.flatten.map(fromInternal))

////////////////// INTERNAL parsing..
private[formson] class FormsonParser(rawMap: FlatFormValues) {

  def parse(): FormDataInternal.Obj = {

    // for every key we get an AST (object) with possibly recursive values
    val objects = rawMap.map { case (key, values) =>
      parseInternal(key, values)
    }.toSeq

    // then we merge all of them to one object
    mergeObjects(objects)
  }

  private def merge(acc: FormDataInternal, second: FormDataInternal): FormDataInternal = (acc, second) match {

    case (FormDataInternal.Simple(_), FormDataInternal.Simple(_)) =>
      // we keep the first value
      // throw exc ?
      acc

    case (FormDataInternal.Obj(existingValuesMap), FormDataInternal.Obj(valuesMap)) =>
      val objAcc = existingValuesMap.to(mutable.SortedMap)
      valuesMap.foreach { case (key, value) =>
        objAcc.get(key) match
          case None =>
            objAcc(key) = value
          case Some(existingValue) =>
            objAcc(key) = merge(existingValue, value)
      }
      FormDataInternal.Obj(objAcc.toMap)

    case (FormDataInternal.Sequence(existingValuesMap), FormDataInternal.Sequence(valuesMap)) =>
      val seqAcc = existingValuesMap.to(mutable.SortedMap)
      valuesMap.foreach { case (idx, values) =>
        seqAcc.get(idx) match
          case None => seqAcc(idx) = values
          case Some(existingValues) =>
            seqAcc(idx) = existingValues ++ values
      }
      FormDataInternal.Sequence(seqAcc.to(SortedMap))

    case (first, second) =>
      throw new FormsonParsingException(s"Unmergeable objects: ${first.tpe} and ${second.tpe} ")
  }

  private def mergeObjects(flatObjects: Seq[FormDataInternal.Obj]): FormDataInternal.Obj = {
    flatObjects
      .foldLeft(FormDataInternal.Obj(Map.empty)) { case (acc, next) =>
        merge(acc, next)
      }
      .asInstanceOf[FormDataInternal.Obj]
  }

  private def parseInternal(rawKey: String, values: Seq[FormValue]): FormDataInternal.Obj = {

    val idxRegex = "(.*)\\[(.*)\\]".r

    split(rawKey) match {
      case (key, None) =>
        key match
          case idxRegex(k, idxstr) =>
            val valuesAsData = values.map(FormDataInternal.Simple.apply)
            val idx = idxstr.toIntOption.getOrElse(0)
            val fd = FormDataInternal.Sequence(SortedMap(idx -> valuesAsData))
            val fdValues = Map(k -> fd)
            FormDataInternal.Obj(fdValues)
          case _ =>
            val fdValues = Map(key -> FormDataInternal.Simple(values.head))
            FormDataInternal.Obj(fdValues)
      case (key, Some(remainingRawKey)) =>
        key match
          case idxRegex(k, idxstr) =>
            val valuesAsData = parseInternal(remainingRawKey, values)
            val idx = idxstr.toIntOption.getOrElse(0)
            val fd = FormDataInternal.Sequence(SortedMap(idx -> Seq(valuesAsData)))
            val fdValues = Map(k -> fd)
            FormDataInternal.Obj(fdValues)
          case _ =>
            val fdValues = Map(key -> parseInternal(remainingRawKey, values))
            FormDataInternal.Obj(fdValues)
    }
  }

  // TODO abc[mykey]
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
