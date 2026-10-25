---
title: Servers and platforms
description: Sharaf server modules and platform support
---

# {{ page.title }}

| Module | Server integration | Platform | Support tier | Form bodies |
| --- | --- | --- | --- | --- |
| `sharaf-undertow` | Undertow | JVM | Stable | URL encoded and multipart |
| `sharaf-http4s` | http4s | JVM, Scala Native | Beta | URL encoded |
| `sharaf-jdk-httpserver` | JDK HTTP server | JVM | Beta | URL encoded |
| `sharaf-helidon` | Helidon | JVM | Experimental | URL encoded; multipart unsupported |
| `sharaf-snunit` | snunit | Scala Native | Experimental | URL encoded; multipart unsupported |

`sharaf-core` supports JVM and Scala Native. The companion libraries `formson`, `querson`, and `validson` also support Scala.js.

Native executable examples cover three deployment paths:

- [GraalVM Native Image with JDK HTTP server]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-jdk-graalvm.scala)
- [Scala Native with http4s/Ember]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-http4s-native.scala)
- [Scala Native with snunit and external Unit]({{site.data.project.gh.sourcesUrl}}/examples/snunit)

The GraalVM example covers the JDK adapter and its example dependencies; it does not establish Native Image compatibility
for every JVM adapter. The snunit example requires `libunit` at build time and a separately configured Unit daemon.

Stable is the recommended server integration. Beta integrations support the listed platforms but have narrower form handling. Experimental integrations have incomplete request features and may change between releases.

The [quickstart](/tutorials/quickstart.html) uses Undertow. Adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).

The [AWS Lambda starter](https://github.com/sake92/sharaf-aws-lambda-starter) packages an Undertow/JVM application
with AWS Lambda Web Adapter and includes Terraform configuration and local integration tests.
