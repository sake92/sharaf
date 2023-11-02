package ba.sake.validson

extension [T](value: T)(using validator: Validator[T]) {

  def validateOrThrow: T =
    val res = validate
    if res.isEmpty then value
    else throw ValidationException(res)

  def validate: Seq[ValidationError] =
    validator.validate(value).map(_.withPathPrefix("$"))

}
