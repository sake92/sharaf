package ba.sake.sharaf

import org.openjdk.jmh.annotations.*

/** Measures `Request.bodyString` on a body stream without bulk-read overrides, as served by Potateh 0.6.0. */
@State(Scope.Thread)
class BodyReadBenchmark {

  // 3 B (HttpArena baseline POST), 512 B form/JSON, 4 KB, 64 KB
  @Param(Array("3", "512", "4096", "65536"))
  var size: Int = 0

  private var body: Array[Byte] = Array.emptyByteArray
  private var headers: Map[HttpString, Seq[String]] = Map.empty

  @Setup def prepare(): Unit = {
    body = Array.fill(size)('7'.toByte)
    headers = Map(
      HttpString("Content-Type") -> Seq("text/plain"),
      HttpString("Content-Length") -> Seq(size.toString)
    )
  }

  /** A fresh request per operation, because the decoded body is cached on the request. */
  @Benchmark def bodyString(): String = BenchRequest(headers, body).bodyString
}
