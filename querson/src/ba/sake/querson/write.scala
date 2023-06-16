package ba.sake.querson

import QueryStringData.*

private[querson] def writeToRawQS(path: String, qsData: QueryStringData, config: Config): RawQueryString = qsData match
  case simple: Simple => Map(path -> Seq(simple.value))
  case seq: Sequence  => writeSeq(path, seq, config)
  case obj: Obj       => writeObj(path, obj, config)

private def writeObj(path: String, qsDataObj: Obj, config: Config): RawQueryString = {
  val acc = scala.collection.mutable.Map.empty[String, Seq[String]]

  qsDataObj.values.foreach { case (k, v) =>
    val subPath =
      if path.isBlank then k
      else
        config.objWriteMode match
          case ObjWriteMode.Brackets => s"$path[$k]"
          case ObjWriteMode.Dots     => s"$path.$k"

    acc ++= writeToRawQS(subPath, v, config)
  }

  acc.toMap
}

private def writeSeq(path: String, qsDataSeq: Sequence, config: Config): RawQueryString = {
  val acc = scala.collection.mutable.Map.empty[String, Seq[String]].withDefaultValue(Seq.empty)

  qsDataSeq.values.zipWithIndex.foreach { case (v, i) =>
    val subPath = config.seqWriteMode match
      case SeqWriteMode.NoBrackets    => path
      case SeqWriteMode.EmptyBrackets => s"$path[]"
      case SeqWriteMode.Brackets      => s"$path[$i]"

    val newValues = writeToRawQS(subPath, v, config)
    newValues.foreach { case (k, newValue) =>
      acc(k) = acc(k) ++ newValue
    }
  }

  acc.toMap
}
