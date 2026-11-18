---
layout: howto.html
title: CORS
description: Sharaf How To CORS
---

# {{ page.title }}

CORS middleware is disabled by default on the server route convenience methods, so requests do not pay for CORS
processing unless you configure it.

If you want it to be available for other domains, 
use the `corsSettings` parameter to set desired config:
```scala
import ba.sake.sharaf.potateh.PotatehSharafServer

val corsSettings = CorsSettings.default.withAllowedOrigins(Set("https://example.com"))

val server = PotatehSharafServer(
    routes, host = "localhost", port = port,
    corsSettings = corsSettings
  )
server.start()
```
