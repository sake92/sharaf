package ba.sake.sharaf

import javax.net.ssl.SSLContext

/** An initialized TLS context containing the application's server credentials and trust settings. */
final class TlsConfig(val context: SSLContext) {
  require(context != null, "TLS context must not be null")
}
