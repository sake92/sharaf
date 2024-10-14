package ba.sake.formson
// TODO rename folder
import java.util.UUID
import java.nio.file.Path

enum Color derives FormDataRW:
  case Red
  case Blue

sealed trait Sealed1 derives FormDataRW
object Sealed1 {
  case class Case1(str: String, integer: Int) extends Sealed1
  case object Case2 extends Sealed1
}

case class NestedSealed1(nest: Sealed1) derives FormDataRW

// @ TODO
enum Annot1 derives FormDataRW:
  case A
  case B(x: String)

case class FormSimple(str: String, strOpt: Option[String], int: Int, uuid: UUID, file: Path, bytes: Array[Byte])
    derives FormDataRW
case class FormSimpleReservedChars(`what%the&stu$f?@[]`: String) derives FormDataRW

case class FormEnum(color: Color) derives FormDataRW

case class FormSeq(a: Seq[String]) derives FormDataRW
case class FormSeqSeq(a: Seq[Seq[String]]) derives FormDataRW

case class FormNested(search: String, p: Page) derives FormDataRW
case class Page(number: Int, size: Int) derives FormDataRW

// Option and Seq have global defaults (in typeclass instance)
case class FormDefaults(q: String = "default", opt: Option[String], seq: Seq[String]) derives FormDataRW
