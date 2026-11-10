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

Native executable examples cover three deployment paths:

- [GraalVM Native Image with Undertow]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-undertow-graalvm.scala)
- [Scala Native with http4s/Ember]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/hello-http4s-native.scala)
- [Scala Native with snunit and external Unit]({{site.data.project.gh.sourcesUrl}}/examples/snunit)

The GraalVM example covers Undertow and its example dependencies; it does not establish Native Image compatibility
for every JVM adapter. The snunit example requires `libunit` at build time and a separately configured Unit daemon.

Local size, startup, and memory figures are listed in the [native footprint reference](/reference/native-footprint.html).

Stable is the recommended server integration. Beta integrations support the listed platforms with less production coverage. Experimental integrations have incomplete request features and may change between releases.

The [quickstart](/tutorials/quickstart.html) uses Undertow. Adapter examples are in the [repository]({{site.data.project.gh.sourcesUrl}}/examples).

The [AWS Lambda starter](https://github.com/sake92/sharaf-aws-lambda-starter) packages an Undertow/JVM application
with AWS Lambda Web Adapter and includes Terraform configuration and local integration tests.

## Query strings and URL-encoded forms

Potateh, JDK HTTP server, Helidon and snunit share raw query and
`application/x-www-form-urlencoded` parsing. Names and values are decoded once
with UTF-8 after splitting on `&` and the first `=`. Repeated fields, empty names
and empty values are retained; empty separator entries are ignored. Form keys
retain their first insertion order. `+` represents a space; `%2B` represents `+`.
Form media types are case-insensitive and may include parameters.

Malformed percent escapes return 400 through the configured exception mapper.
Unsupported form media types return 415. Malformed UTF-8
bytes are replaced with `�`, following Java's
[URLDecoder contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/URLDecoder.html)
and the UTF-8 replacement policy in the
[web form parsing standard](https://url.spec.whatwg.org/#application/x-www-form-urlencoded-parsing).
Broken percent escapes are rejected more strictly than that standard's parser.
Potateh previously rejected malformed query/form UTF-8 with 400; it now uses the
same replacement policy as the other three adapters.

Undertow and http4s retain their server-library URL-encoded parsers. Path decoding is separate
and currently differs between adapters.

## Multipart forms

JDK HTTP server, Potateh, Helidon, snunit and http4s share a byte-oriented
`multipart/form-data` parser. Text fields are decoded as UTF-8. Every part with a
`filename` parameter becomes a temporary `FormValue.File(Path)`, including empty
files. Repeated fields and the first insertion order of keys are retained.
Upload filenames and content types are not exposed by `FormDataMap`.

The parser streams uploads to disk without converting the body to text. snunit
provides its body as an existing byte array; the shared parser wraps that array
as a stream, but does not remove snunit's transport-level buffering.

| Limit | Value |
| --- | --- |
| Total multipart body, including framing and epilogue | 32 MiB |
| Each uploaded file | 16 MiB |
| Each text field | 64 KiB |
| Parts | 128 |
| Headers per part, including line endings | 16 KiB |
| Header count per part | 32 |
| Boundary-line padding lookahead | 16 KiB |

These parser limits are fixed defaults. Server and proxy limits may be lower.
Malformed or truncated multipart input returns 400; exceeding a parser limit
returns 413 through the default or JSON exception mapper. Unsupported part
transfer encodings such as base64 are rejected with 400; identity encodings
(`binary`, `8bit`, `7bit`) leave the bytes unchanged.

Temporary uploads are deleted on parsing failures and after the adapter finishes
response serialization, including handler and writer failures. Potateh 0.2.0
has no exchange-completion hook: its uploads are deleted when the synchronous
Sharaf handler returns. Read or copy Potateh uploads in the handler; do not use
an upload path as a deferred response body. All adapters require an
application-owned copy for background work or permanent storage.

Undertow retains its own parser, limits, and cleanup. Its default threshold
stores nonempty uploads on disk but represents empty uploads as `ByteArray`.
Applications configuring in-memory Undertow uploads retain that behavior.

See [upload a file](/howtos/upload-file.html) for typed `Path` parsing.

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

Malformed query/form percent escapes and invalid path UTF-8 return 400. Unsupported form
media types return 415. Valid but unsupported HTTP methods
return 501. Routes use the configured exception mapper for these adapter errors;
with an already composed handler, errors before context creation use the default
mapper. Malformed HTTP syntax rejected by Potateh uses its own protocol response.

Paths split on literal slashes before percent decoding each segment once. `%2F`
stays within one segment and `+` remains literal. Empty interior segments are
preserved; trailing slashes follow the existing split behavior. This policy is
specific to Potateh; path decoding currently differs between server adapters.
