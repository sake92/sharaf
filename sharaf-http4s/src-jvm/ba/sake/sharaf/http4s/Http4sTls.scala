package ba.sake.sharaf.http4s

import ba.sake.sharaf.TlsConfig
import cats.effect.Async
import fs2.io.net.tls.TLSContext

object Http4sTls {
  def context[F[_]: Async](tls: TlsConfig): TLSContext[F] =
    TLSContext.Builder.forAsync[F].fromSSLContext(tls.context)
}
