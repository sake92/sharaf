package ba.sake.sharaf.jdkhttp

import ba.sake.sharaf.*

extension (r: Request)
  def underlying: JdkHttpServerSharafRequest =
    r.asInstanceOf[JdkHttpServerSharafRequest]
