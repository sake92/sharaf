package ba.sake.sharaf

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

enum ServerSentEvent {
  case Comment(value: String)
  case Message(
      data: String,
      id: Option[String] = None,
      event: Option[String] = None,
      retry: Option[Int] = None
  )
  case Done(event: String = "stop")

  def sseString: String = this match {
    case ServerSentEvent.Comment(value) =>
      s":${value}\n\n"
    case msg: ServerSentEvent.Message =>
      val dataStrings = msg.data.split("\n").map { dataLine =>
        s"data: ${dataLine}"
      }
      val msgStr = List(
        msg.id.map(i => s"id: ${i}"),
        msg.event.map(e => s"event: ${e}"),
        Some(dataStrings.mkString("\n")),
        msg.retry.map(r => s"retry: ${r}")
      ).flatten.mkString("\n")
      s"${msgStr}\n\n"
    case Done(event) =>
      s"""event: ${event}
         |data:\n\n""".stripMargin
  }

  def sseBytes: Array[Byte] = sseString.getBytes(StandardCharsets.UTF_8)
}

class SseSender private (
    private[sharaf] val queue: java.util.concurrent.LinkedBlockingQueue[ServerSentEvent],
    private val _onComplete: () => Unit,
    private val _onError: Throwable => Unit,
    private val _pingInterval: Option[FiniteDuration]
) {
  // Runtime mutable state — inherently not part of the immutable configuration
  @volatile private var _isDone: Boolean = false
  @volatile private var _pingThread: Thread = null

  def send(event: ServerSentEvent): Unit =
    queue.put(event)

  /** Returns a new [[SseSender]] with the given callback invoked when all events have been sent
   *  successfully (after a [[ServerSentEvent.Done]] is delivered).
   *  Should be called before passing the [[SseSender]] to [[Response.withBody]].
   */
  def onComplete(callback: () => Unit): SseSender =
    new SseSender(queue, callback, _onError, _pingInterval)

  /** Returns a new [[SseSender]] with the given callback invoked when an error occurs while
   *  sending events, e.g. when the client disconnects.
   *  Should be called before passing the [[SseSender]] to [[Response.withBody]].
   */
  def onError(callback: Throwable => Unit): SseSender =
    new SseSender(queue, _onComplete, callback, _pingInterval)

  /** Returns a new [[SseSender]] configured to send a [[ServerSentEvent.Comment]] ping every
   *  [[interval]] once streaming starts.  This allows detecting client disconnections even when
   *  no regular events are being sent.  The first ping is sent after one [[interval]] has elapsed.
   *  The ping thread stops automatically when the sender completes or errors.
   *  Should be called before passing the [[SseSender]] to [[Response.withBody]].
   */
  def withPingInterval(interval: FiniteDuration): SseSender =
    new SseSender(queue, _onComplete, _onError, Some(interval))

  /** Starts the ping thread if a ping interval was configured.  Called by [[ResponseWritable]]
   *  when streaming begins.  Idempotent — safe to call more than once.
   */
  private[sharaf] def initPingThread(): Unit =
    if _pingThread == null then
      _pingInterval.foreach { interval =>
        val t = new Thread(() => {
          try {
            while !_isDone do {
              Thread.sleep(interval.toMillis)
              queue.put(ServerSentEvent.Comment("ping"))
            }
          } catch {
            case _: InterruptedException => () // stop cleanly
          }
        })
        t.setDaemon(true)
        _pingThread = t // assign before start to avoid race with invokeOnComplete/invokeOnError
        t.start()
      }

  private[sharaf] def invokeOnComplete(): Unit = {
    _isDone = true
    if _pingThread != null then _pingThread.interrupt()
    _onComplete()
  }

  private[sharaf] def invokeOnError(e: Throwable): Unit = {
    _isDone = true
    if _pingThread != null then _pingThread.interrupt()
    _onError(e)
  }
}

object SseSender {
  def apply(): SseSender = new SseSender(
    new java.util.concurrent.LinkedBlockingQueue[ServerSentEvent],
    () => (),
    _ => (),
    Some(1.second)
  )
}
