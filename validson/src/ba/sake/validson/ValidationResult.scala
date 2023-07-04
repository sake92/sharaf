package ba.sake.validson

case class ValidationError(
    path: String,
    msg: String,
    value: Any
) {
  def withPath(p: String): ValidationError = copy(path = p)
  def withPathPrefix(prefix: String): ValidationError = copy(path = s"${prefix}$path")
}
