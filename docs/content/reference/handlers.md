---
layout: reference.html
title: Request handlers
description: Sharaf handler factories, defaults, and composition
---

# {{ page.title }}

A `SharafHandler` takes a `RequestContext` and returns a `Response[?]`. The companion object provides these factories.

| Factory | Behavior |
| --- | --- |
| `routes(routes, notFoundHandler)` | Dispatches matching application routes and delegates unmatched requests to the fallback. |
| `files(directoryPath, notFoundHandler)` | Serves GET and HEAD from an existing filesystem directory and delegates missing files to the fallback. |
| `classpathResources(rootPath, notFoundHandler)` | Serves packaged resources and delegates misses to the fallback. |
| `exceptions(next, exceptionMapper)` | Maps exceptions from the wrapped handler using `ExceptionMapper.default` unless a mapper is supplied. |
| `cors(next, corsSettings)` | Handles CORS using `CorsSettings.default` by default. Passing `CorsSettings.disabled` returns the wrapped handler unchanged. Server routes convenience methods disable CORS by default. |
| `gzip(next, settings)` | Streams gzip compression for eligible responses when the client accepts gzip. See [response compression](/howtos/response-compression.html) for media types and settings. |
| `sessions(next, store, config)` | Enables sessions with `SessionConfig.default` and an in-memory store by default. Pass `Some(store)` to provide a session store. |

Routes, files, and classpath resources default to `DefaultNotFoundHandler`, which returns `404 Not Found`. A fallback receives the original request.

This composition tries application routes first, then packaged assets, then files on disk. The outer exception handler covers all three. The filesystem directory named `public` must exist at startup.

```scala
import java.nio.file.Paths
import ba.sake.sharaf.*

val handler = SharafHandler.exceptions(
  SharafHandler.routes(
    routes,
    SharafHandler.classpathResources("public", SharafHandler.files(Paths.get("public")))
  )
)
```

The optional JVM module `sharaf-pac4j` provides `Pac4jSecurityHandler` for authentication and authorization. It dispatches the configured callback and logout paths using `Pac4jCallbackHandler` and `Pac4jLogoutHandler`. See [production security](/howtos/security.html) for configuration and persistent sessions.
