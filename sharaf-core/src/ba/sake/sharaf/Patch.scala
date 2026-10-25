package ba.sake.sharaf

import ba.sake.tupson.{JsonRW, TupsonException}
import org.typelevel.jawn.ast.JValue

/** Whether a JSON object field should be updated. Use `Patch[Option[T]]` for nullable fields. */
enum Patch[+T]:
  case Set(value: T)
  case Keep

object Patch:
  given [T](using valueRW: JsonRW[T]): JsonRW[Patch[T]] with
    override def write(value: Patch[T]): JValue = value match
      case Set(value) => valueRW.write(value)
      case Keep       => throw TupsonException("Patch.Keep can only be written as an object field")

    override def shouldWriteField(value: Patch[T]): Boolean = value != Keep

    override def parse(path: String, jValue: JValue): Patch[T] =
      Set(valueRW.parse(path, jValue))

    override def default: Option[Patch[T]] = Some(Keep)
