---
title: Redirect
description: Sharaf How To Redirect
---

# {{ page.title }}


Use the `Response.redirect` function:
```scala
case GET -> Path("a-deprecated-route") =>
    Response.redirect("/this-other-place")
```

This will redirect the request to "/this-other-place",  
with status `301 MOVED_PERMANENTLY`
