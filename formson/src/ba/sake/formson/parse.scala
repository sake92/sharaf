package ba.sake.formson

import scala.collection.mutable
import scala.collection.immutable.SortedMap
import fastparse.Parsed.Success
import fastparse.Parsed.Failure

/** Takes a raw map of form data and converts it into a JSON-like AST
  *
  * @param formDataMap
  *   Raw map of form data
  * @return
  *   Form data AST
  */

private[formson] def parseFDMap(formDataMap: FormDataMap): FormData =
  val parser = FormsonParser(formDataMap)
  val formDataInternal = parser.parse()
  fromInternal(formDataInternal)

private def fromInternal(fdi: FormDataInternal): FormData = fdi match
  case FormDataInternal.Simple(value)       => FormData.Simple(value)
  case FormDataInternal.Obj(values)         => FormData.Obj(values.view.mapValues(fromInternal).toMap)
  case FormDataInternal.Sequence(valuesMap) => FormData.Sequence(valuesMap.values.toSeq.flatten.map(fromInternal))

// internal, temporary representation
private[formson] enum FormDataInternal(val tpe: String):
  case Simple(value: FormValue) extends FormDataInternal("simple value")
  case Sequence(values: SortedMap[Int, Seq[FormDataInternal]]) extends FormDataInternal("sequence")
  case Obj(values: Map[String, FormDataInternal]) extends FormDataInternal("object")

////////////////// INTERNAL parsing..
private[formson] class FormsonParser(formDataMap: FormDataMap) {
  import FormDataInternal.*

  def parse(): Obj =
    // for every key we get an AST (object) with possibly recursive values
    val objects = formDataMap.map { case (key, values) =>
      val keyParts = KeyParser(key).parse()
      parseInternal(keyParts, values).asInstanceOf[Obj]
    }.toSeq
    // then we merge all of them to one object
    mergeObjects(objects)

  private def merge(acc: FormDataInternal, second: FormDataInternal): FormDataInternal = (acc, second) match {

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
      throw FormsonException(s"Unmergeable objects: ${first.tpe} and ${second.tpe}")
  }

  private def mergeObjects(flatObjects: Seq[Obj]): Obj =
    flatObjects
      .foldLeft(Obj(Map.empty)) { case (acc, next) =>
        merge(acc, next)
      }
      .asInstanceOf[Obj]

  private def parseInternal(keyParts: Seq[String], values: Seq[FormValue]): FormDataInternal = {

    keyParts match
      case Seq(key, rest: _*) =>
        val adaptedKey = if key.isBlank then "0" else key
        adaptedKey.toIntOption match
          case Some(index) =>
            if rest.isEmpty
            then Sequence(SortedMap(index -> values.map(Simple.apply)))
            else Sequence(SortedMap(index -> Seq(parseInternal(rest, values))))

          case None =>
            if rest.isEmpty then Obj(Map(key -> Sequence(SortedMap(0 -> values.map(Simple.apply)))))
            else Obj(Map(key -> parseInternal(rest, values)))

      case Seq() => throw FormsonException("Empty key parts")
  }

}

private[formson] class KeyParser(key: String) {
  import fastparse._, NoWhitespace._

  private val ForbiddenKeyChars = Set('[', ']', '.')

  def parse(): Seq[String] =
    val res = fastparse.parse(key, parseFinal(_))
    res match
      case Success((firstKey, subKeys), index) => subKeys.prepended(firstKey)
      case f: Failure                          => throw FormsonException(f.msg)

  private def parseFinal[$: P] = P(
    Start ~ parseKey ~ (parseBracketedSubKey | parseDottedSubKey | parseIndex).rep(min = 0) ~ End
  )

  private def parseKey[$: P] = P(CharPred(c => !ForbiddenKeyChars(c) && !c.isWhitespace).rep(min = 1).!)

  private def parseBracketedSubKey[$: P] = P("[" ~ parseKey ~ "]")

  private def parseDottedSubKey[$: P] = P("." ~ parseKey)

  private def parseIndex[$: P] = P("[" ~ CharIn("0-9").rep(min = 0).! ~ "]")

}
