package ba.sake.sharaf

final class QueryString(
    val params: Seq[(String, Seq[String])]
) {
  override def toString(): String =
    val p = params
      .map((k, values) => values.map(v => s"${k}=${v}").mkString("&"))
      .mkString("&")
    s"QueryString($p)"
}

object QueryString {
  def apply(params: (String, Seq[String])*): QueryString = new QueryString(
    params.toSeq
  )
  def unapplySeq(queryString: QueryString): Option[Seq[(String, Seq[String])]] =
    Some(queryString.params)
}
