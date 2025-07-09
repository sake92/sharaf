---
title: CORS
description: Sharaf How To CORS
---

# {{ page.title }}

By default, Sharaf sets no permitted origins.  
This means you can only use the API/website from the same domain.

If you want it to be available for other domains, 
use the `corsSettings` parameter to set desired config:
```scala
val corsSettings = CorsSettings.default.withAllowedOrigins(Set("https://example.com"))

val server = UndertowSharafServer(
    "localhost",
    port,
    routes,
    corsSettings = corsSettings
  )
```
