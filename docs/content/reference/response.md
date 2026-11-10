---
title: Response
description: Sharaf response API reference
---

# {{ page.title }}

`Response` is an immutable builder. `Response.default` has status `200 OK` and no body.

| Member | Effect |
| --- | --- |
| `Response.withBody(value)` | Creates a response using a `ResponseWritable` for the value's type |
| `Response.withStatus(status)` | Creates a response with the given status |
| `.withStatus(status)` | Replaces the status |
| `.settingHeader(name, value)` | Sets a response header |
| `.removingHeader(name)` | Removes a response header |
| `.settingCookie(cookie)` | Sets a cookie |
| `.removingCookie(name)` | Removes a cookie |
| `.withBody(value)` | Replaces the body |
| `Response.redirect(location)` | `301 Moved Permanently` with a `Location` header |

`ResponseWritable[T]` defines `write(value: T, outputStream: OutputStream): Unit` and `headers(value: T): Seq[(HttpString, Seq[String])]`. Instances include strings, JSON-serializable types, streams, file paths, and server-sent events. See [custom response bodies](/howtos/response-body.html).

Body-generated headers are applied before explicit header updates. Header names are case-insensitive;
updates run in builder order, with the last set or removal taking effect. Setting a sequence replaces
all previous values for that name; an empty sequence removes the header. Repeated values are retained,
and cookie updates append to explicit `Set-Cookie` headers. Explicit headers also apply without a body.
These rules apply to Undertow, Potateh, JDK HTTP server, http4s, and Helidon; transport framing remains
managed by each server.
