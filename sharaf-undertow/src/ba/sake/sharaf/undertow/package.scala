package ba.sake.sharaf.undertow

import ba.sake.sharaf.*

given (using r: Request): Session =
  val undertowReq = r.asInstanceOf[UndertowSharafRequest]
  val s = io.undertow.util.Sessions.getOrCreateSession(undertowReq.underlyingHttpServerExchange)
  UndertowSharafSession(s)

extension (r: Request)
  def underlying: UndertowSharafRequest =
    r.asInstanceOf[UndertowSharafRequest]
