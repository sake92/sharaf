package sttp.client4.curl.sharaf

import sttp.client4.*

object TestBackend {
  lazy val instance: SyncBackend = quick.backend
}
