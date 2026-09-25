---
title: Servers and platforms
description: Sharaf server modules and platform support
---

# {{ page.title }}

| Module | Server integration | Platform |
| --- | --- | --- |
| `sharaf-undertow` | Undertow | JVM |
| `sharaf-http4s` | http4s | JVM, Scala Native |
| `sharaf-helidon` | Helidon | JVM |
| `sharaf-jdk-httpserver` | JDK HTTP server | JVM |
| `sharaf-snunit` | snunit | Scala Native |

`sharaf-core` supports JVM and Scala Native. The companion libraries `formson`, `querson`, and `validson` also support Scala.js.

The [quickstart](/tutorials/quickstart.html) uses Undertow. Working adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).
