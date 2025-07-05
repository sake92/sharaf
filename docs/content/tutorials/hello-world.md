---
title: Hello World
description: Sharaf Tutorial Hello World
---

# {{ page.title }}


Let's make a Hello World example with scala-cli.  
Create a file `hello.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path("hello", name) =>
    Response.withBody(s"Hello $name")

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")
```

Then run it like this:
```sh
scala-cli hello.sc 
```
Go to http://localhost:8181/hello/Bob.  
You will see a "Hello Bob" text response.

---
The most interesting part is the `Routes` definition.  
Here we pattern match on `(HttpMethod, Path)`.  
The `Path` contains a `Seq[String]`, which are the parts of the URL you can match on.

