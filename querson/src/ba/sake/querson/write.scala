package ba.sake.querson

import QueryStringData.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private[querson] def writeToQSMap(path: String, qsData: QueryStringData, config: Config): QueryStringMap = qsData match
  case simple: Simple => Map(path -> Seq(simple.value))
  case seq: Sequence  => writeSeq(path, seq, config)
  case obj: Obj       => writeObj(path, obj, config)

private def writeObj(path: String, qsDataObj: Obj, config: Config): QueryStringMap = {
  val acc = scala.collection.mutable.Map.empty[String, Seq[String]]

  qsDataObj.values.foreach { case (k, v) =>
    val encodedKey = URLEncoder.encode(k, StandardCharsets.UTF_8)
    val subPath =
      if path.isBlank then encodedKey
      else
        config.objWriteMode match
          case ObjWriteMode.Brackets => s"$path[$encodedKey]"
          case ObjWriteMode.Dots     => s"$path.$encodedKey"

    acc ++= writeToQSMap(subPath, v, config)
  }

  acc.toMap
}

private def writeSeq(path: String, qsDataSeq: Sequence, config: Config): QueryStringMap = {
  val acc = scala.collection.mutable.Map.empty[String, Seq[String]].withDefaultValue(Seq.empty)

  qsDataSeq.values.zipWithIndex.foreach { case (v, i) =>
    val subPath = config.seqWriteMode match
      case SeqWriteMode.NoBrackets    => path
      case SeqWriteMode.EmptyBrackets => s"$path[]"
      case SeqWriteMode.Brackets      => s"$path[$i]"

    val newValues = writeToQSMap(subPath, v, config)
    newValues.foreach { case (k, newValue) =>
      acc(k) = acc(k) ++ newValue
    }
  }

  acc.toMap
}
