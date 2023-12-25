package ba.sake.validson

final class ValidsonException(val errors: Seq[ValidationError]) extends Exception(errors.mkString("; "))
