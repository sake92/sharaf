package ba.sake.formson

import java.nio.file.Path
import scala.collection.immutable.SortedMap

enum FormValue(val tpe: String) {
  case Str(value: String) extends FormValue("simple value")
  case File(value: Path) extends FormValue("a file")
}

type FlatFormValues = Map[String, Seq[FormValue]]

// internal representation
enum FormDataInternal(val tpe: String) {
  case Simple(value: FormValue) extends FormDataInternal("simple value")
  case Obj(values: Map[String, FormDataInternal]) extends FormDataInternal("object")
  case Sequence(values: SortedMap[Int, Seq[FormDataInternal]]) extends FormDataInternal("sequence")
}

enum FormData(val tpe: String) {
  case Simple(value: FormValue) extends FormData("simple value")
  case Obj(values: Map[String, FormData]) extends FormData("object")
  case Sequence(values: Seq[FormData]) extends FormData("sequence")
}
