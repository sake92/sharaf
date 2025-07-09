---
title: HTML
description: Sharaf Tutorial HTML
---

# {{ page.title }}

You can make an HTML snippet by using the `html""` interpolator.
Then you return it directly in the `Response.withBody()`.

Let's make a simple HTML page that greets the user.  
Create a file `html.sc` and paste this code into it:

{# need to HTML encode these snippets, so that Markdown doesnt process them! #}
{% set index_view = 'html"""
    <!DOCTYPE html>
    <html lang="en">
    <body>
    <div>
        <p>Welcome!</p>
        <a href="/hello/Bob">Hello world</a>
    </div>
    </body>
    </html>
  """'
%}
{% set hello_view = 'html"""
    <!DOCTYPE html>
    <html lang="en">
    <body>
    <div>
        Hello <b>${name}</b>!
    </div>
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
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView =
  {{ index_view | e }}

def HelloView(name: String) =
  {{ hello_view | e }}
```

and run it like this:
```sh
scala html.sc 
```

Go to http://localhost:8181  
to see how it works.
