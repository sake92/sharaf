package ba.sake.sharaf.handlers

import java.io.{BufferedInputStream, ByteArrayOutputStream}
import java.net.{InetSocketAddress, Socket}
import java.nio.charset.StandardCharsets.{UTF_8, ISO_8859_1}

// sttp has bug in handling options on scalanative
private[sharaf] object HttpConnection {
  type Response = (status: Int, headers: Map[String, String], body: String)
}

/** Raw HTTP/1.1 exposes framing and persistent connections without client normalization or buffering. */
private[sharaf] final class HttpConnection(address: InetSocketAddress) extends AutoCloseable {
  val socket = new Socket()
  try {
    socket.connect(address, 5000)
    socket.setSoTimeout(15000)
  } catch {
    case scala.util.control.NonFatal(error) =>
      socket.close()
      throw error
  }
  private val input = new BufferedInputStream(socket.getInputStream)
  private val output = socket.getOutputStream

  def request(
      method: String,
      target: String,
      body: String = "",
      contentType: String = "",
      headers: Seq[(String, String)] = Seq.empty
  ): HttpConnection.Response = {
    send(method, target, body, contentType, headers)
    readResponse(method == "HEAD")
  }

  def send(
      method: String,
      target: String,
      body: String = "",
      contentType: String = "",
      headers: Seq[(String, String)] = Seq.empty
  ): Unit = {
    val bytes = body.getBytes(UTF_8)
    val contentHeader = if contentType.isEmpty then "" else s"Content-Type: $contentType\r\n"
    val extraHeaders = headers.map { case (name, value) => s"$name: $value\r\n" }.mkString
    output.write(
      s"$method $target HTTP/1.1\r\nHost: localhost\r\nContent-Length: ${bytes.length}\r\n$contentHeader$extraHeaders\r\n"
        .getBytes(ISO_8859_1)
    )
    if bytes.nonEmpty then output.write(bytes)
    output.flush()
  }

  def remaining(): String = new String(input.readAllBytes(), UTF_8)
  def readBytes(size: Int): String = new String(input.readNBytes(size), UTF_8)

  def line(): String = {
    val result = new ByteArrayOutputStream()
    var next = input.read()
    while next != '\n' && next != -1 do {
      if next != '\r' then result.write(next)
      next = input.read()
    }
    if next == -1 then throw new java.io.EOFException("Unexpected connection close")
    result.toString("ISO-8859-1")
  }

  def readResponse(head: Boolean = false): HttpConnection.Response = {
    val status = line().split(" ")(1).toInt
    val headers = Map.newBuilder[String, String]
    var header = line()
    while header.nonEmpty do {
      val parts = header.split(":", 2)
      headers += parts(0).toLowerCase(java.util.Locale.ROOT) -> parts(1).trim
      header = line()
    }
    val fields = headers.result()
    val body = new ByteArrayOutputStream()
    if !head && !Set(204, 205, 304).contains(status) then {
      if fields.get("transfer-encoding").contains("chunked") then {
        var size = Integer.parseInt(line().takeWhile(_ != ';'), 16)
        while size != 0 do {
          body.write(input.readNBytes(size))
          if line().nonEmpty then throw new java.io.IOException("Invalid chunk terminator")
          size = Integer.parseInt(line(), 16)
        }
        if line().nonEmpty then throw new java.io.IOException("Invalid chunk terminator")
      } else body.write(input.readNBytes(fields.getOrElse("content-length", "0").toInt))
    }
    (status = status, headers = fields, body = body.toString("UTF-8"))
  }

  override def close(): Unit = socket.close()
}
