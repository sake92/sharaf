package ba.sake.sharaf.handlers

import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

/** Native sttp 4.0.27 truncates binary bodies (softwaremill/sttp#3043). The curl executable sends exact bytes without
  * adding a Native TLS dependency to these HTTP-only tests.
  */
private[sharaf] object MultipartRequests {
  def withClient(base: String)(check: ((String, Array[Byte]) => (status: Int, body: Array[Byte])) => Unit): Unit = {
    val directory = Files.createTempDirectory("sharaf-multipart-http-")
    val upload = directory.resolve("request.bin")
    val download = directory.resolve("response.bin")
    try
      check { (path, body) =>
        Files.write(upload, body)
        val process = new ProcessBuilder(
          "curl",
          "--silent",
          "--show-error",
          "--http1.1",
          "--max-time",
          "5",
          "--header",
          "Content-Type: multipart/form-data; boundary=\"upload-boundary\"",
          "--data-binary",
          s"@$upload",
          "--output",
          download.toString,
          "--write-out",
          "%{http_code}",
          s"$base/$path"
        ).redirectErrorStream(true).start()
        try {
          val output = new String(process.getInputStream.readAllBytes(), UTF_8)
          val exitCode = process.waitFor()
          if exitCode != 0 then throw new IOException(s"curl exited with $exitCode: $output")
          (status = output.toInt, body = Files.readAllBytes(download))
        } finally {
          if process.isAlive() then process.destroyForcibly()
          process.waitFor()
        }
      }
    finally {
      Files.deleteIfExists(upload)
      Files.deleteIfExists(download)
      Files.delete(directory)
    }
  }
}
