package ba.sake.sharaf.handlers

import ba.sake.sharaf.*
import scala.concurrent.duration.*
import sttp.client4.SyncBackend
import sttp.client4.quick.quickRequest
import sttp.model.HttpVersion
import sttp.client4.curl.sharaf.TestBackend

/** Each adapter supplies its real server; shared suites own the HTTP contract. */
trait ServerFixture {
  protected val request = quickRequest.httpVersion(HttpVersion.HTTP_1_1).readTimeout(5.seconds)
  protected def httpBackend: SyncBackend = TestBackend.instance

  def withHandler(handler: SharafHandler)(check: String => Unit): Unit

  final def withServer(routes: Routes, mapper: ExceptionMapper)(check: String => Unit): Unit =
    withHandler(SharafHandler.exceptions(SharafHandler.routes(routes), mapper))(check)
}
