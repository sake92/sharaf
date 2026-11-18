---
layout: reference.html
title: Servers and platforms
description: Sharaf server modules and platform support
---

# {{ page.title }}

| Module | Server integration | Platform | Support tier | Form bodies |
| --- | --- | --- | --- | --- |
| `sharaf-undertow` | Undertow | JVM | Stable | URL encoded and multipart |
| `sharaf-http4s` | http4s | JVM, Scala Native | Beta | URL encoded and multipart |
| `sharaf-potateh` | Potateh | JVM, Scala Native | Experimental | URL encoded and multipart |
| `sharaf-snunit` | snunit | Scala Native | Experimental | URL encoded and multipart |

`sharaf-core` supports JVM and Scala Native. The companion libraries `formson`, `querson`, and `validson` also support Scala.js.

Native executable examples cover these deployment paths:

- [GraalVM Native Image with Undertow]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-undertow-graalvm.scala)
- [GraalVM Native Image with Potateh]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-potateh-graalvm.scala)
- [Scala Native with http4s/Ember]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-http4s-native.scala)
- [Scala Native with Potateh]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-potateh-native.scala)
- [Scala Native with snunit and external Unit]({{site.data.project.gh.sourcesUrl}}/examples/snunit)

The GraalVM examples cover their respective servers and example dependencies; they do not establish Native Image
compatibility for every JVM adapter or application dependency. The snunit example requires `libunit` at build time
and a separately configured Unit daemon.

Local size, startup, and memory figures are listed in the [native footprint reference](/reference/native-footprint.html).

Potateh is the default integration used in the introductory examples. Undertow remains the stable integration;
Potateh is experimental and may change between releases. Beta integrations support the listed platforms with less
production coverage.

The [quickstart](/tutorials/quickstart.html) uses Potateh. Adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).

The [AWS Lambda starter](https://github.com/sake92/sharaf-aws-lambda-starter) packages an Undertow/JVM application
with AWS Lambda Web Adapter and includes Terraform configuration and local integration tests.

## Choosing a port

Potateh accepts port `0` in its bind address to let the operating system choose a port. Read the bound port from the server:

```scala
import java.net.InetSocketAddress
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.potateh.PotatehSharafServer

val config = ServerConfig.Default.withBindAddress(new InetSocketAddress("127.0.0.1", 0))
val server = PotatehSharafServer.start(config, routes)
println(server.address.getPort)
```

Pass an explicit port for a fixed address. Undertow exposes `server.port` after starting; with http4s Ember,
use port `0` and read `server.address.port.value`.

Request parsing behavior and verification are described in the [parsing reference](/reference/parsing.html).

## TLS

On the JVM, `TlsConfig` holds an initialized `javax.net.ssl.SSLContext` with the application's server
credentials and trust settings. The same configuration can be passed to `UndertowSharafServer` as `tls = Some(config)`,
`PotatehTls.configure`, or `Http4sTls.context[IO]` (for Ember's `withTLS`). Potateh serves HTTP/1;
its TLS context must not negotiate HTTP/2 via ALPN.

The http4s integration supplies an application handler; the application owns its server and TLS listener.
The snunit integration runs behind a separately configured Unit daemon, which owns TLS configuration.
The shared JVM configuration is unavailable on Scala Native. Native TLS depends on the server and its
platform-specific TLS provider or an external TLS terminator.
