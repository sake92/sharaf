---
title: Path Params
description: Sharaf Tutorial Path Params
---

# {{ page.title }}

Path parameters can be extracted from the `Path(segments: Seq[String])` argument.

Create a file `path_params.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("string", x) =>
    Response.withBody(s"string = ${x}")

  case GET -> Path("int", param[Int](x)) =>
    Response.withBody(s"int = ${x}")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
```

Then run it like this:
```sh
scala-cli path_params.sc 
```

---
Now go to [http://localhost:8181/string/abc](http://localhost:8181/string/abc)
and you will get the param returned: `string = abc`.

When you go to [http://localhost:8181/int/123](http://localhost:8181/int/123),  
Sharaf will *try to extract* an `Int` from the path parameter.  
If it doesn't match, it will fall through, try the next route.
    
