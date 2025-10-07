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

  def sseString: String = this match {
    case ServerSentEvent.Comment(value) =>
      s":${value}\n\n"
    case msg: ServerSentEvent.Message =>
      val msgStr = List(
        msg.id.map(i => s"id: ${i}"),
        msg.event.map(e => s"event: ${e}"),
        Some(s"data: ${msg.data}"),
        msg.retry.map(r => s"retry: ${r}")
      ).flatten.mkString("\n")
      s"${msgStr}\n\n"
  }

  def sseBytes: Array[Byte] = sseString.getBytes(StandardCharsets.UTF_8)
}
