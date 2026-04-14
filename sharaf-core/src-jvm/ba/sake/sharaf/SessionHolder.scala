package ba.sake.sharaf

/** Thread-local session holder for JVM request-scoped session access. */
private[sharaf] object SessionHolder {

  private val threadLocal = new ThreadLocal[Option[Session]]()

  def get: Option[Session] = Option(threadLocal.get()).flatten

  def set(session: Session): Unit = threadLocal.set(Some(session))

  def clear(): Unit = threadLocal.remove()
}
