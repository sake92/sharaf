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

Stable is the recommended server integration. Beta integrations support the listed platforms but have narrower form handling. Experimental integrations have incomplete request features and may change between releases.

The [quickstart](/tutorials/quickstart.html) uses Undertow. Adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).
