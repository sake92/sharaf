package ba.sake.sharaf

/** Global session holder for Scala Native.
  *
  * SNUnit processes requests one at a time per worker process (single-threaded event loop),
  * so a plain mutable variable is safe here. This must not be used in a multi-threaded
  * environment.
  */
private[sharaf] object SessionHolder {

  private var _session: Option[Session] = None

  def get: Option[Session] = _session

  def set(session: Session): Unit = _session = Some(session)

  def clear(): Unit = _session = None
}
