package ba.sake.sharaf

final class Path private (
    val segments: Seq[String]
) {
  override def toString(): String =
    val p = segments.mkString("/")
    s"Path($p)"

  override def equals(that: Any): Boolean =
    that match {
      case that: Path =>
        this.segments == that.segments
      case _ => false
    }

  override def hashCode(): Int =
    segments.hashCode()
}

object Path:
  def apply(segments: String*): Path = new Path(segments.toSeq)

  def unapplySeq(path: Path): Option[Seq[String]] = Some(path.segments)
