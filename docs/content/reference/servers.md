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

Stable is the recommended server integration. Beta integrations support the listed platforms with less production coverage. Experimental integrations have incomplete request features and may change between releases.

The [quickstart](/tutorials/quickstart.html) uses Undertow. Adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).

The [AWS Lambda starter](https://github.com/sake92/sharaf-aws-lambda-starter) packages an Undertow/JVM application
with AWS Lambda Web Adapter and includes Terraform configuration and local integration tests.

## Choosing a port

The Undertow wrapper defaults to `127.0.0.1` and port `0`, which lets the operating system choose a port. Read `server.port` after starting:

```scala
val server = UndertowSharafServer(routes)
server.start()
println(server.port)
```

Potateh also accepts port `0` in its bind address and exposes the bound port through
`server.address.getPort`. With http4s Ember, use port `0` and read `server.address.port.value`.
Pass an explicit port for a fixed address.

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
