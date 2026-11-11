---
title: Request parsing
description: Request parsing behavior and verification
---

# {{ page.title }}

## Cookies

All adapters expose request cookies through `Request.current.cookies` and support
`Response.settingCookie` and `Response.removingCookie`. Multiple response cookies
are sent as separate `Set-Cookie` headers, including on responses without a body.

Supported response attributes are `path`, `domain`, `maxAge`, `expires`, `secure`,
`httpOnly`, and `sameSiteMode` (`Strict`, `Lax`, or `None`). Cookie values are opaque;
`+` and percent escapes are not URL-decoded. Removal sends an empty value with an
expiry in the past; cookie path and domain must match the cookie being removed.
For a scoped cookie, `settingCookie` with the matching path/domain and a past
`expires` value supplies that scope.

Undertow, Potateh, JDK HTTP server, Helidon, and snunit share request-cookie
parsing; http4s uses its engine parser. The shared parser accepts up to 200 named
cookie pairs, including duplicates, across all Cookie headers. It returns 400 via
the configured exception mapper if the limit is exceeded. Undertow uses its
configured `MAX_COOKIES` value instead. This limit applies when cookies are read.

## Query strings and URL-encoded forms

Undertow, Potateh, JDK HTTP server, Helidon and snunit share raw query and
`application/x-www-form-urlencoded` parsing. Names and values are decoded once
with UTF-8 after splitting on `&` and the first `=`. Repeated fields, empty names
and empty values are retained; empty separator entries are ignored. Form keys
retain their first insertion order. `+` represents a space; `%2B` represents `+`.
Form media types are case-insensitive and may include parameters.

Undertow and Potateh limit URL-encoded request bodies to 1 MiB when reading
`bodyString` or parsing a form. Larger bodies return 413 through the configured
exception mapper. Other adapters do not apply this Sharaf-side limit.

Malformed percent escapes return 400 through the configured exception mapper.
Unsupported form media types return 415. Malformed UTF-8
bytes are replaced with `�`, following Java's
[URLDecoder contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/URLDecoder.html)
and the UTF-8 replacement policy in the
[web form parsing standard](https://url.spec.whatwg.org/#application/x-www-form-urlencoded-parsing).
Broken percent escapes are rejected more strictly than that standard's parser.
Potateh previously rejected malformed query/form UTF-8 with 400; it now uses the
same replacement policy as the other adapters.

http4s uses its server-library query parser and the shared URL-encoded form parser.
Undertow, http4s, JDK HTTP server, Helidon and Potateh split raw paths into segments
before decoding each segment once. Encoded slashes remain within their segment;
`+` remains a literal plus in paths.

## Multipart forms

All adapters share a byte-oriented
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

Temporary uploads are deleted on parsing failures and after response processing,
including handler and writer failures. Potateh uses its response-completion hook
so uploads remain available to deferred response writers and are also cleaned
when the engine skips the body. All adapters require an application-owned copy
for background work or permanent storage.

Undertow uses the shared parser and limits, including `FormValue.File(Path)` for
empty uploads. Undertow-specific in-memory upload thresholds do not apply to
Sharaf form parsing.

See [upload a file](/howtos/upload-file.html) for typed `Path` parsing.

## Verification

These local checks complement Sharaf's automated tests. They cover selected
inputs and configurations, not complete protocol conformance.

### URL-encoded parsing vectors

In November 2026, the shared query and form parsers on the JVM and Scala Native were checked against the
35 inputs in [Web Platform Tests' URL-encoded parser cases](https://github.com/web-platform-tests/wpt/blob/130c0e258c7ae25bb852dc543129031a9915d9ca/url/urlencoded-parser.any.js).
Both matched the expected grouped values for 28 inputs. For the other seven,
both rejected malformed percent escapes that the browser-oriented tests accept,
consistent with Sharaf's documented stricter behavior.

The same vectors were also sent over HTTP/1.1 to Undertow 2.4.3.Final on the
JVM and Potateh 0.2.1-SNAPSHOT on the JVM and Scala Native. All three returned
the expected grouped values for 28 form bodies and rejected the seven malformed
escapes with 400. For queries, 26 matched and the same seven returned 400; the
two inputs containing literal non-ASCII characters were excluded from raw
request targets.

These checks did not run the original browser test harness, verify global pair
ordering, or cover the full URL test corpus.

### HTTP/1.1 framing comparison

In November 2026, [HTTP Garden](https://github.com/narfindustries/http-garden/tree/b417e806c1b15e8ea0b9312f81a91fbdcbc7a83e)
compared its Undertow echo handler using Undertow 2.4.3.Final with a Sharaf
Potateh 0.2.1-SNAPSHOT echo handler on the JVM and Scala Native. For eight
selected HTTP/1.1 cases, Garden reported no differences: all accepted a GET,
fixed-length and chunked bodies, and a two-request pipeline; all returned 400
for duplicate identical or conflicting content lengths, combined content length
and chunked encoding, and whitespace before a header colon.

The Undertow comparison used Garden's handler directly, without the Sharaf
Undertow adapter. The temporary Sharaf echo handler covered only the selected
paths, HTTP/1.1 and ASCII bodies. This was a small direct-server check, not
fuzzing, proxy testing or a complete HTTP conformance assessment.

### Multipart forms in a browser

In November 2026, Playwright drove Chromium 156.0.8078.4 to submit four multipart
forms to Undertow 2.4.3.Final on the JVM and Potateh 0.2.1-SNAPSHOT on the JVM
and Scala Native. All returned the expected fields and file bytes for empty inputs,
Unicode and reserved field names, a binary upload, and multiple files under one name. The scenarios were
inspired by [Rack multifail](https://github.com/rack/multifail); its generator was
not run. Uploaded filenames and content types were not checked.

This check did not cover other browsers, legacy charsets, or malformed
multipart bodies.

### Cookie parsing comparison

In November 2026, Sharaf Potateh on the JVM and Scala Native and Werkzeug 3.1.8
received the same 59,319 generated cookie headers. The inputs used the
three-character name alphabet from [Cookie Crumbles](https://github.com/SecPriv/cookiecrumbles/tree/main/reflectors),
with a fixed value of `foo`. Both returned the same sorted name/value pairs for all
cases, with no request errors. The comparison used an adapted harness, not the
upstream fuzzer or its older reflector versions.

In a separate comparison of 16 selected headers on both platforms, the parsers agreed on 13. The
three differences involved quoted values: Sharaf preserves the quotes, while
Werkzeug removes them; for a quoted value containing a semicolon, Sharaf also
splits at the semicolon. These differences alone do not establish a defect.

The generated comparison did not check duplicate headers, ordering, other values,
or conformance against a specification. Clients and servers may
normalize headers before parsing.
