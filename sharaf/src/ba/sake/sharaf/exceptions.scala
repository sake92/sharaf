package ba.sake.sharaf

class SharafException(msg: String) extends Exception(msg)

//
final class ValidationException(
    val errors: Seq[ValidationError]
) extends SharafException(s"Validation errors: ${errors.mkString("[", ";", "]")}")

object ValidationException {
  def apply(errors: Seq[ValidationError]): ValidationException =
    new ValidationException(errors)
  def apply(name: String, reason: String): ValidationException =
    new ValidationException(Seq(ValidationError(reason, Some(name))))
  def apply(reason: String): ValidationException =
    new ValidationException(Seq(ValidationError(reason, None)))
}

final case class ValidationError(
    reason: String,
    name: Option[String] = None
) {
  override def toString(): String =
    val nameStr = name.map(n => s"$n: ").getOrElse("")
    s"$nameStr$reason"
}

///////
object Validation {
  def assertAll(
      assertions: (String, Boolean, String)*
  ): Unit = {
    val failures = assertions.filter((_, b, _) => !b).map((name, b, msg) => ValidationError(msg, Some(name)))
    if failures.nonEmpty then throw ValidationException(failures)
  }
}
