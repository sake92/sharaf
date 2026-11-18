package ba.sake.sharaf.potateh

import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.TlsConfig

object PotatehTls {
  def configure(config: ServerConfig, tls: TlsConfig): ServerConfig =
    config.withServerSocketFactory(() => tls.context.getServerSocketFactory.createServerSocket())
}
