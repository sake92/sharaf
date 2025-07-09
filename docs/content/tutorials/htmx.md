---
title: HTMX
description: Sharaf Tutorial HTMX
---

# {{ page.title }}

[HTMX]("https://htmx.org/") is an incredibly simple, HTML-first library.  
Instead of going through HTML->JS->JSON-API loop/mess, you can go directly HTML->HTML-API.  
Basically you just return HTML snippets that get included where you want in your page.

You can lots of examples in [examples/htmx]({{site.data.project.gh.sourcesUrl}}/examples/htmx) folder.

---

Let's make a simple page that triggers a POST request to fetch a HTML snippet.  
Create a file `htmx_load_snippet.sc` and paste this code into it:

{# need to HTML encode these snippets, so that Markdown doesnt process them! #}
{% set div_snippet = 'html"""
        <div>
        <b>WOW, it works! 😲</b>
        <div>Look ma, no JS! 😎</div>
        </div>
      """'
%}
{% set index_view = 'html"""
    <!DOCTYPE html>
    <html>
    <head>
      <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    </head>
    <body>
      <button hx-post="/html-snippet" hx-swap="outerHTML">Click here!</button>
    </body>
    </html>
  """'
%}


```scala
//> using scala "3.7.0"
//> using dep {{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}

import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case POST -> Path("html-snippet") =>
    Response.withBody:
      {{ div_snippet | e }}

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView =
  {{ index_view | e }}

```

and run it like this:
```sh
scala html.sc 
```

Go to [http://localhost:8181](http://localhost:8181)  
to see how it works.


