package ba.sake.sharaf.undertow

import ba.sake.sharaf.*

extension (r: Request)
  def underlying: UndertowSharafRequest =
    r.asInstanceOf[UndertowSharafRequest]
