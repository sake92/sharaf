---
title: Servers and platforms
description: Sharaf server modules and platform support
---

# {{ page.title }}

| Module | Server integration | Platform | Support tier | Form bodies |
| --- | --- | --- | --- | --- |
| `sharaf-undertow` | Undertow | JVM | Stable | URL encoded and multipart |
| `sharaf-http4s` | http4s | JVM, Scala Native | Beta | URL encoded and multipart |
| `sharaf-jdk-httpserver` | JDK HTTP server | JVM, Scala Native | Beta | URL encoded and multipart |
| `sharaf-helidon` | Helidon | JVM | Experimental | URL encoded and multipart |
| `sharaf-potateh` | Potateh | JVM, Scala Native | Experimental | URL encoded and multipart |
| `sharaf-snunit` | snunit | Scala Native | Experimental | URL encoded and multipart |

`sharaf-core` supports JVM and Scala Native. The companion libraries `formson`, `querson`, and `validson` also support Scala.js.

On Scala Native, JDK HTTP server uses an explicitly composed `SharafHandler`.
Its routes convenience overload wires classpath resources and requires JVM APIs.

The JDK HTTP server uses a ten-thread pool by default and shuts it down when stopped. To supply your own
`java.util.concurrent.Executor`, pass `Some(executor)` when creating the server; you own its shutdown.

Native executable examples cover three deployment paths:

- [GraalVM Native Image with Undertow]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-undertow-graalvm.scala)
- [Scala Native with http4s/Ember]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-http4s-native.scala)
- [Scala Native with Potateh]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-potateh-native.scala)
- [Scala Native with snunit and external Unit]({{site.data.project.gh.sourcesUrl}}/examples/snunit)

The GraalVM example covers Undertow and its example dependencies; it does not establish Native Image compatibility
for every JVM adapter. The snunit example requires `libunit` at build time and a separately configured Unit daemon.

Local size, startup, and memory figures are listed in the [native footprint reference](/reference/native-footprint.html).

Stable is the recommended server integration. Beta integrations support the listed platforms with less production coverage. Experimental integrations have incomplete request features and may change between releases.

The [quickstart](/tutorials/quickstart.html) uses Undertow. Adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).

The [AWS Lambda starter](https://github.com/sake92/sharaf-aws-lambda-starter) packages an Undertow/JVM application
with AWS Lambda Web Adapter and includes Terraform configuration and local integration tests.

## Choosing a port

Undertow, Helidon, and JDK HTTP server wrappers accept port `0` to let the operating system
choose a port. Their overloads without a port do the same. Read `server.port` after starting:

```scala
val server = UndertowSharafServer("127.0.0.1", routes)
server.start()
println(server.port)
```

Potateh also accepts port `0` in its bind address and exposes the bound port through
`server.address.getPort`. With http4s Ember, use port `0` and read `server.address.port.value`.
Pass an explicit port for a fixed address.

Request parsing behavior and verification are described in the [parsing reference](/reference/parsing.html).
