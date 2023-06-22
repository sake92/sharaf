package ba.sake.formson

import FormData.*

private[formson] def writeToFDMap(path: String, formData: FormData, config: Config): FormDataMap = formData match
  case simple: Simple => Map(path -> Seq(simple.value))
  case seq: Sequence  => writeSeq(path, seq, config)
  case obj: Obj       => writeObj(path, obj, config)

private def writeObj(path: String, formDataObj: Obj, config: Config): FormDataMap = {
  val acc = scala.collection.mutable.Map.empty[String, Seq[FormValue]]

  formDataObj.values.foreach { case (key, v) =>
    val subPath =
      if path.isBlank then key
      else
        config.objWriteMode match
          case ObjWriteMode.Brackets => s"$path[$key]"
          case ObjWriteMode.Dots     => s"$path.$key"

    acc ++= writeToFDMap(subPath, v, config)
  }

  acc.toMap
}

private def writeSeq(path: String, formDataSeq: Sequence, config: Config): FormDataMap = {
  val acc = scala.collection.mutable.Map.empty[String, Seq[FormValue]].withDefaultValue(Seq.empty)

  formDataSeq.values.zipWithIndex.foreach { case (v, i) =>
    val subPath = config.seqWriteMode match
      case SeqWriteMode.NoBrackets    => path
      case SeqWriteMode.EmptyBrackets => s"$path[]"
      case SeqWriteMode.Brackets      => s"$path[$i]"

    val newValues = writeToFDMap(subPath, v, config)
    newValues.foreach { case (k, newValue) =>
      acc(k) = acc(k) ++ newValue
    }
  }

  acc.toMap
}
