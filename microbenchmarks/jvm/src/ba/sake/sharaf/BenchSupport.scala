package ba.sake.sharaf

import java.io.{InputStream, OutputStream}

import ba.sake.querson.QueryStringMap

/** Minimal in-memory request; the body is exposed through [[FixedLengthBodyStream]]. */
final class BenchRequest(override val headers: Map[HttpString, Seq[String]], body: Array[Byte]) extends Request {
  override def queryParamsRaw: QueryStringMap = Map.empty
  override def bodyInputStream: InputStream = new FixedLengthBodyStream(body)
}

/** Overrides the same methods as Potateh 0.6.0's `FixedLengthInputStream` (`read`, `read(Array, Int, Int)`, `skip`,
  * `available`, `close`), so `readAllBytes`, `readNBytes` and `transferTo` use the JDK `InputStream` defaults. Unlike
  * `ByteArrayInputStream`, which copies exactly the remaining bytes.
  */
final class FixedLengthBodyStream(bytes: Array[Byte]) extends InputStream {
  private var position = 0

  override def read(): Int =
    if position >= bytes.length then -1
    else {
      val value = bytes(position) & 0xff
      position += 1
      value
    }

  override def read(buffer: Array[Byte], offset: Int, length: Int): Int =
    if length == 0 then 0
    else if position >= bytes.length then -1
    else {
      val count = math.min(length, bytes.length - position)
      System.arraycopy(bytes, position, buffer, offset, count)
      position += count
      count
    }

  override def skip(count: Long): Long = {
    val skipped = math.max(0L, math.min(count, (bytes.length - position).toLong)).toInt
    position += skipped
    skipped.toLong
  }

  override def available(): Int = bytes.length - position
}

/** Discards output and counts bytes, so benchmarks measure serialization rather than I/O. */
final class CountingSink extends OutputStream {
  var count = 0L
  override def write(b: Int): Unit = count += 1
  override def write(b: Array[Byte], off: Int, len: Int): Unit = count += len
}

object BenchSupport {

  /** Writes the response body the way a server adapter does. */
  def writeBody[T](response: Response[T], output: OutputStream): Unit =
    response.body.foreach(value => response.rw.write(value, output))

  /** JSON shaped like HttpArena's json-comp items. */
  def jsonItems(items: Int): String =
    (0 until items)
      .map { i =>
        s"""{"id":$i,"name":"Item $i","category":"electronics","price":${1000 + i},"quantity":${i % 7},""" +
          s""""active":${i % 2 == 0},"tags":["fast","new"],"rating":{"score":${i % 5},"count":${i * 3}},"total":0}"""
      }
      .mkString("""{"items":[""", ",", s"""],"count":$items}""")
}
