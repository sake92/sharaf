---
title: Hello World
description: Sharaf Tutorial Hello World
---

# {{ page.title }}


Let's make a Hello World example with scala-cli.  
Create a file `hello.sc` and paste this code into it:
```scala
{% include "hello.sc" %}
```

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

