package ba.sake.formson

import java.nio.file.Path

enum FormValue(val tpe: String) {
  case Str(value: String) extends FormValue("simple value")
  case File(value: Path) extends FormValue("a file")
  case ByteArray(value: Array[Byte]) extends FormValue("byte array")
}

/** Represents a raw form data map. Values are not encoded.
  */
type FormDataMap = Map[String, Seq[FormValue]]

enum FormData(val tpe: String):

  case Simple(value: FormValue) extends FormData("simple value")

  case Sequence(values: Seq[FormData]) extends FormData("sequence")

  case Obj(values: Map[String, FormData]) extends FormData("object")
