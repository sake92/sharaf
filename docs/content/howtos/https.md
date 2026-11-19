---
layout: howto.html
title: Serve HTTPS on the JVM
description: Configure TLS for Sharaf with Undertow, Potateh, or http4s Ember
---

# {{ page.title }}

Use a PKCS#12 keystore containing the server's private key and certificate chain. Set `TLS_KEYSTORE_PASSWORD` through your deployment's secret configuration. This example uses the same password for the keystore and the private key.

```scala
import java.nio.file.{Files, Paths}
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scala.util.Using
import ba.sake.sharaf.*

val password = sys.env("TLS_KEYSTORE_PASSWORD").toCharArray
val keyStore = KeyStore.getInstance("PKCS12")
Using.resource(Files.newInputStream(Paths.get("server.p12"))) { input =>
  keyStore.load(input, password)
}
val keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
keys.init(keyStore, password)
val context = SSLContext.getInstance("TLS")
context.init(keys.getKeyManagers, null, null)
val tls = TlsConfig(context)

val routes = Routes {
  case GET -> Path() => Response.withBody("secure")
}
```

### Undertow

```scala
import ba.sake.sharaf.undertow.UndertowSharafServer

val server = UndertowSharafServer(routes, host = "localhost", port = 8443, tls = Some(tls))
server.start()
```

### Potateh

Set the bind address on the `ServerConfig` passed to `withConfig`. Potateh serves HTTP/1 over TLS.

```scala
import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.potateh.{PotatehSharafServer, PotatehTls}

val config = ServerConfig.Default.withBindAddress(new InetSocketAddress("localhost", 8443))
val server = PotatehSharafServer(routes, host = "localhost", port = 8443)
  .withConfig(PotatehTls.configure(config, tls))
server.start()
```

### http4s Ember

In your application's `IOApp`, use the server resource with your configured `LoggerFactory[IO]`.

```scala
import ba.sake.sharaf.http4s.{Http4sTls, SharafHttpApp}
import cats.effect.IO
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder

val serve: IO[Unit] = EmberServerBuilder
  .default[IO]
  .withHost(host"localhost")
  .withPort(port"8443")
  .withTLS(Http4sTls.context[IO](tls))
  .withHttpApp(SharafHttpApp(SharafHandler.routes(routes)))
  .build
  .use(_ => IO.never[Unit])
```

Request `https://localhost:8443/` with a certificate issued for `localhost` by a CA your client trusts. For a local CA, pass its certificate with `curl --cacert ca.pem https://localhost:8443/`.

See [servers and platforms](/reference/servers.html#tls) for Scala Native and snunit TLS ownership.
