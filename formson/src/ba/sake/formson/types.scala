package ba.sake.formson

import java.nio.file.Path
import scala.collection.immutable.SeqMap

enum FormValue(val tpe: String) {
  case Str(value: String) extends FormValue("simple value")
  case File(value: Path) extends FormValue("file")
  case ByteArray(value: Array[Byte]) extends FormValue("byte array")
}

/** Represents a raw form data map. Keys are ordered by insertion order. Values are not encoded.
  */
type FormDataMap = SeqMap[String, Seq[FormValue]]

enum FormData(val tpe: String):

  case Simple(value: FormValue) extends FormData("simple value")

  case Sequence(values: Seq[FormData]) extends FormData("sequence")

  case Obj(values: SeqMap[String, FormData]) extends FormData("object")
