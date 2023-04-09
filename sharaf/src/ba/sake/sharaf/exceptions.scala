package ba.sake.sharaf

class SharafException(msg: String) extends Exception(msg)

class ValidationException(
    val errors: List[ValidationError]
) extends SharafException(s"Validation errors: ${errors.mkString("[", ";", "]")}")

case class ValidationError(
    name: String,
    reason: String
) {
  override def toString(): String =
    s"$name: $reason"
}
