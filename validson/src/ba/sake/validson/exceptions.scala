package ba.sake.validson

class ValidationException(val errors: Seq[ValidationError]) extends Exception(errors.mkString("; "))
