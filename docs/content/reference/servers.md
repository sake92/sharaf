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
| `sharaf-potateh` | Potateh | JVM, Scala Native | Experimental | URL encoded; multipart unsupported |
| `sharaf-snunit` | snunit | Scala Native | Experimental | URL encoded; multipart unsupported |

`sharaf-core` supports JVM and Scala Native. The companion libraries `formson`, `querson`, and `validson` also support Scala.js.

Native executable examples cover three deployment paths:

- [GraalVM Native Image with Undertow]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-undertow-graalvm.scala)
- [Scala Native with http4s/Ember]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-http4s-native.scala)
- [Scala Native with snunit and external Unit]({{site.data.project.gh.sourcesUrl}}/examples/snunit)

The GraalVM example covers Undertow and its example dependencies; it does not establish Native Image compatibility
for every JVM adapter. The snunit example requires `libunit` at build time and a separately configured Unit daemon.

Local size, startup, and memory figures are listed in the [native footprint reference](/reference/native-footprint.html).

Stable is the recommended server integration. Beta integrations support the listed platforms but have narrower form handling. Experimental integrations have incomplete request features and may change between releases.

The [quickstart](/tutorials/quickstart.html) uses Undertow. Adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).

The [AWS Lambda starter](https://github.com/sake92/sharaf-aws-lambda-starter) packages an Undertow/JVM application
with AWS Lambda Web Adapter and includes Terraform configuration and local integration tests.

## Potateh

`PotatehSharafServer.start(config, routes)` and `start(config, handler)` return a
Potateh `Server`. The caller stops it with `server.stop()`. Routes can also receive
CORS settings, an exception mapper and a not-found handler.

The overloads `start(config, routes, executor)` and `start(config, handler, executor)`
accept a caller-owned `java.util.concurrent.Executor`. A routes overload also accepts
all middleware settings together with an executor. Stopping the server leaves the
executor open. Each submitted task serves a whole persistent connection; a small
fixed pool can starve other clients. Rejected submissions must throw.

Potateh owns connection and transfer framing. The adapter removes engine-owned and
Connection-nominated headers, translates an explicit `Content-Length` to Potateh's
writer length, and streams bodies without buffering them. Invalid or contradictory
response lengths fail as server errors. Length mismatches during streaming terminate
the connection. Response-requested `Connection: close`, upgrades and trailers are
unsupported and fail as server errors. SSE uses chunked streaming on HTTP/1.1.

Malformed query/form encoding and invalid path UTF-8 return 400. Unsupported form
media types, including multipart, return 415. Valid but unsupported HTTP methods
return 501. Routes use the configured exception mapper for these adapter errors;
with an already composed handler, errors before context creation use the default
mapper. Malformed HTTP syntax rejected by Potateh uses its own protocol response.

Paths split on literal slashes before percent decoding each segment once. `%2F`
stays within one segment and `+` remains literal. Empty interior segments are
preserved; trailing slashes follow the existing split behavior. This policy is
specific to Potateh; path decoding currently differs between server adapters.
