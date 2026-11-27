package ba.sake.sharaf.potateh

import org.openjdk.jmh.annotations.*

import ba.sake.sharaf.*

/** Measures Sharaf-to-Potateh response conversion, including Potateh's `Headers.from` validation.
  * It is in this package because `PotatehConversions` is `private[potateh]`.
  */
@State(Scope.Thread)
class ToResponseBenchmark {

  @Param(Array("text", "json_with_headers"))
  var shape: String = ""

  private var response: Response[String] = null

  @Setup def prepare(): Unit =
    response = shape match {
      case "text" => Response.withBody("3")
      case _      =>
        Response
          .withBody("""{"ok":true}""")
          .settingHeader("Content-Type", "application/json")
          .settingHeader("Cache-Control", "no-cache")
          .settingHeader("Content-Length", "11")
          .settingHeader("X-Request-Id", "abc-123")
    }

  @Benchmark def toResponse(): ba.sake.potateh.Response = PotatehConversions.toResponse(response, "GET")
}
