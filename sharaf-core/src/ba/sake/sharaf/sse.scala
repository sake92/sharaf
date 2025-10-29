package ba.sake.sharaf

import java.nio.charset.StandardCharsets

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

class SseSender {
  private[sharaf] val queue = java.util.concurrent.LinkedBlockingQueue[ServerSentEvent]
  def send(event: ServerSentEvent): Unit =
    queue.put(event)

  // TODO add onComplete, onError
}
