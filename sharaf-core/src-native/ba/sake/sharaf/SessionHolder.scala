package ba.sake.sharaf

/** Global session holder for Scala Native (SNUnit is single-threaded per worker). */
private[sharaf] object SessionHolder {

  private var _session: Option[Session] = None

  def get: Option[Session] = _session

  def set(session: Session): Unit = _session = Some(session)

  def clear(): Unit = _session = None
}
