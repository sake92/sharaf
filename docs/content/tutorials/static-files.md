---
layout: tutorial.html
title: Static Files
description: Sharaf Tutorial Static Files
---

# {{ page.title }}

The example serves static files from the `resources/public` folder on the classpath using
`SharafHandler.classpathResources` as its not-found handler:
- in Mill those are under `my_project/resources/public`
- in sbt those are under `src/main/resources/public`
- in scala you need to manually tell it where to look for with `--resource-dir resources`

---

Let's serve an `example.js` file with Sharaf.  
First create a file `resources/public/example.js`.  
Put this text into it: `console.log('Hello Sharaf!');`.

Now create a file `static_files.sc` and paste this code into it:
<!-- example:start examples/scala-cli/static_files.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody("Try http://localhost:8181/example.js")

PotatehSharafServer(routes, host = "localhost", port = 8181, notFoundHandler = SharafHandler.classpathResources("public")).start()

println(s"Server started at http://localhost:8181")
```
<!-- example:end -->

and run it like this:
```sh
scala static_files.sc  --resource-dir resources
```

Go to http://localhost:8181/example.js.  
You will see the `example.js` contents served.
