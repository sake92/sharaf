---
layout: tutorial.html
title: Hello World
description: Sharaf Tutorial Hello World
---

# {{ page.title }}


Let's make a Hello World example with scala-cli.  
Create a file `hello.sc` and paste this code into it:
<!-- example:start examples/scala-cli/hello.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")
  case GET -> _ =>
    Response.withBody("Go to http://localhost:8181/hello/your-name")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println(s"Server started at http://localhost:8181")
```
<!-- example:end -->

Then run it like this:
```sh
scala hello.sc 
```
Go to http://localhost:8181/hello/Bob.  
You will see a "Hello Bob" text response.

---
The most interesting part is the `Routes` definition.  
Here we pattern match on `(HttpMethod, Path)`.  
The `Path` contains a `Seq[String]`, which are the parts of the URL you can match on.
