package ba.sake.sharaf.handlers

import scala.concurrent.duration.*
import sttp.client4.*

private[sharaf] object MultipartRequests {
  def withClient(base: String)(check: ((String, Array[Byte]) => (status: Int, body: Array[Byte])) => Unit): Unit = {
    val backend = DefaultSyncBackend()
    try
      check { (path, body) =>
        val response = quickRequest
          .httpVersion(sttp.model.HttpVersion.HTTP_1_1)
          .post(uri"$base/$path")
          .contentType("multipart/form-data; boundary=\"upload-boundary\"")
          .body(body)
          .response(asByteArrayAlways)
          .readTimeout(5.seconds)
          .send(backend)
        (status = response.code.code, body = response.body)
      }
    finally backend.close()
  }
}
