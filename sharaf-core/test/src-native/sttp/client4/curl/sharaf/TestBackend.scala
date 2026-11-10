package sttp.client4.curl.sharaf

import scala.scalanative.unsafe.*
import sttp.client4.*
import sttp.client4.curl.AbstractSyncCurlBackend
import sttp.client4.curl.internal.{Curl, CurlApi, CurlCode}
import sttp.client4.wrappers.FollowRedirectsBackend
import sttp.monad.IdentityMonad

/** sttp 4.0.27 omits @blocking on curl_easy_perform. Client and server share a Native runtime in these tests, so GC
  * must be able to suspend the client while it waits for a response.
  */
object TestBackend {
  lazy val instance: SyncBackend = FollowRedirectsBackend(new BlockingCurlBackend)
}

private class BlockingCurlBackend extends AbstractSyncCurlBackend(IdentityMonad, false) with SyncBackend {
  override def performCurl(handle: CurlApi.CurlHandle): CurlCode.CurlCode = CurlCode(BlockingCurl.perform(handle))
}

@extern
private object BlockingCurl {
  @blocking
  @name("curl_easy_perform")
  def perform(handle: Ptr[Curl]): CInt = extern
}
