---
layout: tutorial.html
title: Path Params
description: Sharaf Tutorial Path Params
---

# {{ page.title }}

Path parameters can be extracted from the `Path(segments: Seq[String])` argument.

Create a file `path_params.sc` and paste this code into it:
<!-- example:start examples/scala-cli/path_params.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("string", x) =>
    Response.withBody(s"string = ${x}")
  case GET -> Path("int", param[Int](x)) =>
    Response.withBody(s"int = ${x}")
  case GET -> _ =>
    Response.withBody("Try http://localhost:8181/int/123 or http://localhost:8181/string/123abc")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")
```
<!-- example:end -->

Then run it like this:
```sh
scala path_params.sc 
```

---
Now go to [http://localhost:8181/string/abc](http://localhost:8181/string/abc)
and you will get the param returned: `string = abc`.

When you go to [http://localhost:8181/int/123](http://localhost:8181/int/123),  
Sharaf will *try to extract* an `Int` from the path parameter.  
If it doesn't match, it will fall through, try the next route.
    
