---
title: Native footprint
description: Measured Sharaf executable sizes, startup, and memory usage
---

# {{ page.title }}

Local Linux x86_64 measurements from 2026-10-25 for a small application with plain-text and JSON GET/POST routes.

| Deployment | Packaged MiB | Stripped MiB | First HTTP ms | Idle RSS MiB | After requests RSS MiB |
| --- | ---: | ---: | ---: | ---: | ---: |
| Undertow / GraalVM Native Image | 41.56 | 41.56 | 30.5 | 34.7 | 41.3 |
| Undertow / JVM | 17.71 | — | 599.4 | 94.3 | 105.2 |
| http4s / JVM | 36.70 | — | 1413.8 | 183.2 | 201.8 |
| http4s / Scala Native, release-fast | 22.12 | 18.39 | 30.1 | 30.9 | 43.6 |
| http4s / Scala Native, release-size | 21.81 | 18.00 | 31.8 | 30.5 | 43.2 |

Startup and RSS are medians over five fresh processes on a warm host/filesystem. First HTTP is a successful
`GET /plaintext`; idle RSS is sampled one second later. Post-request RSS is sampled one second after 300 sequential
mixed plain-text/JSON GET/JSON POST requests. Successful requests, Unicode JSON, invalid input (400), and missing
routes (404) were checked over real HTTP. The stripped Scala Native copies passed the same HTTP checks;
GraalVM output is already stripped.

JVM sizes are fat JARs and exclude the JVM. Native executables require OS shared libraries. Stripped sizes use
`strip --strip-unneeded`; startup/RSS figures refer to the original packaged executables. These are small example
applications; the figures do not describe concurrent-load throughput, container size, or Lambda cold starts.

Environment: Intel Core i7-1165G7, Scala CLI 1.16.0, Scala 3.9.0, Sharaf 0.20.0, Temurin 21.0.10,
GraalVM CE 25.0.2 (O2, Serial GC, compatibility CPU target),
http4s/Ember 1.0.0-M48, Scala Native 0.5.12 with multithreading, Immix GC and no LTO. Local Clang 14 emitted a
deprecation warning; Scala Native recommends Clang 16 or newer. The http4s examples explicitly set body headers
for compatibility with published Sharaf 0.20.0.

snunit is excluded: its example compiled, but linking required the missing local `libunit` dependency. A snunit
comparison must include the Unit daemon and application workers together.
