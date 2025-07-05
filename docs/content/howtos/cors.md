---
title: CORS
description: Sharaf How To CORS
---

# {{ page.title }}

By default, Sharaf sets no permitted origins.  
This means you can only use the API/website from the same domain.

If you want to configure it to be available for other domains, 
use the `withCorsSettings` method and set desired config:
```scala
import ba.sake.sharaf.handlers.cors.CorsSettings
import ba.sake.sharaf.*

val corsSettings = CorsSettings.default.withAllowedOrigins(Set("https://example.com"))
UndertowSharafServer(routes).withCorsSettings(corsSettings)...
```
