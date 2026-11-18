---
layout: reference.html
title: Native footprint
description: Measured Sharaf executable sizes, startup, and memory usage
---

# {{ page.title }}

Local Linux x86_64 measurements for a small application with plain-text and JSON GET/POST routes.

| Deployment | Packaged MiB | Stripped MiB | First HTTP ms | Idle RSS MiB | After requests RSS MiB |
| --- | ---: | ---: | ---: | ---: | ---: |
| Potateh / GraalVM Native Image | ~17 MB | ~17 MB | — | — | — |
| Potateh / Scala Native, release-size | 6.48 | 5.11 | 11.8 | 11.2 | 16.3 |
| Undertow / GraalVM Native Image | 41.56 | 41.56 | 30.5 | 34.7 | 41.3 |
| Undertow / JVM | 17.71 | — | 599.4 | 94.3 | 105.2 |
| http4s / JVM | 36.70 | — | 1413.8 | 183.2 | 201.8 |
| http4s / Scala Native, release-size | 21.81 | 18.00 | 31.8 | 30.5 | 43.2 |

The [Potateh GraalVM example]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-potateh-graalvm.scala)
produced a 17 MB executable on Linux x86_64 with GraalVM Community 25.0.2,
Scala 3.9.0, Sharaf 0.23.0, and Potateh 0.5.0. Plain-text and JSON requests succeeded, and malformed JSON
returned 400. The current Sharaf server API with Potateh 0.6.0 produced an executable of the same size and passed
the same checks. Startup and RSS were not measured for these builds.

In the table, startup and RSS are medians over five fresh processes on a warm host/filesystem. First HTTP is a successful
`GET /plaintext`; idle RSS is sampled one second later. Post-request RSS is sampled one second after 300 sequential
mixed plain-text/JSON GET/JSON POST requests. Successful requests, Unicode JSON, invalid input (400), and missing
routes (404) were checked over real HTTP. The stripped Scala Native copies passed the same HTTP checks;
GraalVM output is already stripped. The builds use different Sharaf and compiler versions.

JVM sizes are fat JARs and exclude the JVM. Native executables require OS shared libraries. Stripped sizes use
`strip --strip-unneeded`; startup/RSS figures refer to the original packaged executables. These are small example
applications; the figures do not describe concurrent-load throughput, container size, or Lambda cold starts.
