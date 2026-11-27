package ba.sake.sharaf

import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.Deflater

import org.openjdk.jmh.annotations.*

import ba.sake.sharaf.handlers.GzipHandler
import ba.sake.sharaf.routing.Path

/** Compares the GzipHandler response path with uncompressed output and with compression on a reused Deflater. */
@State(Scope.Thread)
class GzipBenchmark {

  // 8 ≈ 1 KB; 25/50 = HttpArena json-comp sizes; 512 ≈ 64 KB
  @Param(Array("8", "25", "50", "512"))
  var items: Int = 0

  private val sink = new CountingSink
  private var payload: String = ""
  private var payloadBytes: Array[Byte] = Array.emptyByteArray
  private var app: SharafHandler = null
  private var gzipped: SharafHandler = null
  private var context: RequestContext = null

  private val reusedDeflater = new Deflater(3, true)
  private val scratch = new Array[Byte](64 * 1024)

  @Setup def prepare(): Unit = {
    payload = BenchSupport.jsonItems(items)
    payloadBytes = payload.getBytes(UTF_8)
    app = _ => Response.withBody(payload).settingHeader("Content-Type", "application/json")
    gzipped = GzipHandler(GzipSettings.default.withLevel(3), app)
    val request = BenchRequest(Map(HttpString("Accept-Encoding") -> Seq("gzip")), Array.emptyByteArray)
    context = RequestContext((method = HttpMethod.GET, path = Path("json")), request)
  }

  @TearDown def close(): Unit = reusedDeflater.end()

  /** GzipHandler at level 3: handle, then write the compressed body. */
  @Benchmark def gzipHandler(): Long = {
    sink.count = 0
    BenchSupport.writeBody(gzipped.handle(context), sink)
    sink.count
  }

  /** The same response without GzipHandler. */
  @Benchmark def uncompressed(): Long = {
    sink.count = 0
    BenchSupport.writeBody(app.handle(context), sink)
    sink.count
  }

  /** Level 3 compression on a reused raw Deflater: a lower bound for gzipHandler. */
  @Benchmark def deflateOnly(): Int = {
    reusedDeflater.reset()
    reusedDeflater.setInput(payloadBytes)
    reusedDeflater.finish()
    var written = 0
    while !reusedDeflater.finished() do written += reusedDeflater.deflate(scratch)
    written
  }
}
