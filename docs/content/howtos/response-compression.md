---
layout: howto.html
title: Response Compression
description: Sharaf How To Response Compression
---

# {{ page.title }}

Response compression is opt-in. Wrap the application handler with `SharafHandler.gzip`:

```scala
val handler = SharafHandler.exceptions(
  SharafHandler.gzip(SharafHandler.routes(routes)),
  ExceptionMapper.default
)
```

Sharaf compresses eligible text, JSON, XML, JavaScript, and SVG responses when the request accepts gzip. It preserves
`Vary: Accept-Encoding`, skips responses that are already encoded or marked `no-transform`, and does not compress
server-sent events.

Compression streams directly into the response body, so its compressed length is not known in advance. HTTP/1.1
servers can use chunked transfer encoding for those responses.

The default gzip level is 6. Applications that prefer lower CPU use over smaller responses can select another level:

```scala
val settings = GzipSettings.default.withLevel(1)
val handler = SharafHandler.gzip(SharafHandler.routes(routes), settings)
```
