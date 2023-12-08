package ba.sake.sharaf

final class Path(
    val segments: Seq[String]
) {
  override def toString(): String =
    val p = segments.mkString("/")
    s"Path($p)"
}

object Path:
  def apply(segments: String*): Path = new Path(segments.toSeq)

  def unapplySeq(path: Path): Option[Seq[String]] = Some(path.segments)
