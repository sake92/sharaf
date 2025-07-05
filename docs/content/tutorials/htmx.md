---
title: HTMX
description: Sharaf Tutorial HTMX
---

# {{ page.title }}

[HTMX]("https://htmx.org/") is an incredibly simple, HTML-first library.  
Instead of going through HTML->JS->JSON-API loop/mess, you can go directly HTML->HTML-API.  
Basically you just return HTML snippets that get included where you want in your page.

Sharaf is using the [hepek-components](https://sake92.github.io/hepek/hepek/components/reference/bundle-reference.html)
as its template engine, which has support for HTMX attributes.

You can lots of examples in [examples/htmx]({{site.data.project.gh.sourcesUrl}}/examples/htmx) folder.

---

Let's make a simple page that triggers a POST request to fetch a HTML snippet.  
Create a file `htmx_load_snippet.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import scalatags.Text.all.*
import ba.sake.hepek.htmx.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case POST -> Path("html-snippet") =>
    Response.withBody(
      div(
        b("WOW, it works! 😲"),
        div("Look ma, no JS! 😎")
      )
    )

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView = doctype("html")(
  html(
    head(
      script(src := "https://unpkg.com/htmx.org@2.0.4")
    ),
    body(
      button(hx.post := "/html-snippet", hx.swap := "outerHTML")("Click here!")
    )
  )
)
```

and run it like this:
```sh
scala-cli html.sc 
```

Go to [http://localhost:8181](http://localhost:8181)  
to see how it works.


