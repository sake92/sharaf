package ba.sake.sharaf

/** Headers represented as a series of immutable transformations. This is handy when you dynamically remove header(s),
  * maybe set by a previous Undertow handler.
  *
  * @param updates
  *   Series of header transformations
  */
private[sharaf] final case class HeaderUpdates(updates: Seq[HeaderUpdate]) {

  def effective(name: HttpString, bodyHeaders: Seq[(HttpString, Seq[String])]): Option[Seq[String]] = {
    var effectiveValues = Option.empty[Seq[String]]
    bodyHeaders.foreach { case (headerName, values) =>
      if name == headerName then {
        effectiveValues = Option.when(values.nonEmpty)(values)
      }
    }
    updates.foreach {
      case HeaderUpdate.Set(headerName, values) if name == headerName =>
        effectiveValues = Option.when(values.nonEmpty)(values)
      case HeaderUpdate.Remove(headerName) if name == headerName =>
        effectiveValues = None
      case _ =>
        ()
    }
    effectiveValues
  }

  /** Body headers precede explicit updates. Keep removals so adapters can clear headers from earlier handlers. */
  def resolve(bodyHeaders: Seq[(HttpString, Seq[String])]): Seq[HeaderUpdate] = {
    val initial = bodyHeaders.map { case (name, values) => HeaderUpdate.Set(name, values) }
    val resolved = (initial ++ updates).foldLeft(Map.empty[HttpString, HeaderUpdate]) {
      case (headers, HeaderUpdate.Set(name, values)) =>
        val update = if (values.isEmpty) HeaderUpdate.Remove(name) else HeaderUpdate.Set(name, values)
        headers.updated(name, update)
      case (headers, update @ HeaderUpdate.Remove(name)) => headers.updated(name, update)
    }
    resolved.values.toSeq
  }

  def setting(name: HttpString, values: Seq[String]): HeaderUpdates =
    copy(updates = updates.appended(HeaderUpdate.Set(name, values)))

  def setting(name: HttpString, value: String): HeaderUpdates =
    copy(updates = updates.appended(HeaderUpdate.Set(name, Seq(value))))

  def removing(name: HttpString): HeaderUpdates =
    copy(updates = updates.appended(HeaderUpdate.Remove(name)))

}

private[sharaf] enum HeaderUpdate:
  case Set(name: HttpString, values: Seq[String])
  case Remove(name: HttpString)
