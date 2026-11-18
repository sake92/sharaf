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
import ba.sake.potateh.ServerConfig
import ba.sake.sharaf.potateh.PotatehSharafServer

val corsSettings = CorsSettings.default.withAllowedOrigins(Set("https://example.com"))

val server = PotatehSharafServer.start(
    ServerConfig.Default.withBindAddress(new java.net.InetSocketAddress("localhost", port)),
    routes,
    corsSettings = corsSettings
  )
```
