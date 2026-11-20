---
layout: tutorial.html
title: HTMX
description: Sharaf Tutorial HTMX
---

# {{ page.title }}

[HTMX](https://htmx.org/) is an incredibly simple, HTML-first library.
Instead of going through HTML->JS->JSON-API loop/mess, you can go directly HTML->HTML-API.  
Basically you just return HTML snippets that get included where you want in your page.

You can lots of examples in [examples/htmx]({{site.data.project.gh.sourcesUrl}}/examples/htmx) folder.

---

Let's make a simple page that triggers a POST request to fetch a HTML snippet.  
Create a file `htmx_load_snippet.sc` and paste this code into it:

<!-- example:start examples/htmx/htmx_load_snippet.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-undertow:0.21.0

import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case POST -> Path("html-snippet") =>
    Response.withBody:
      html"""
        <div>
        <b>WOW, it works! 😲</b>
        <div>Look ma, no JS! 😎</div>
        </div>
      """

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView =
  html"""
    <!DOCTYPE html>
    <html>
    <head>
      <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    </head>
    <body>
      <button hx-post="/html-snippet" hx-swap="outerHTML">Click here!</button>
    </body>
    </html>
  """
```
<!-- example:end -->

and run it like this:
```sh
scala htmx_load_snippet.sc 
```

Go to [http://localhost:8181](http://localhost:8181)  
to see how it works.
