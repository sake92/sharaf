---
title: HTML
description: Sharaf Tutorial HTML
---

# {{ page.title }}

You can return a scalatags `doctype` directly in the `Response.withBody()`.  
Let's make a simple HTML page that greets the user.  
Create a file `html.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import scalatags.Text.all.*
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")

def IndexView = doctype("html")(
  html(
    p("Welcome!"),
    a(href := "/hello/Bob")("Go to /hello/Bob")
  )
)

def HelloView(name: String) = doctype("html")(
  html(
    p("Welcome!"),
    div("Hello ", b(name), "!")
  )
)
```

and run it like this:
```sh
scala-cli html.sc 
```

Go to http://localhost:8181  
to see how it works.


### Hepek Components
Sharaf supports the [hepek-components](https://sake92.github.io/hepek/hepek/components/reference/bundle-reference.html) too.  
Hepek wraps scalatags with helpful utilities  like Bootstrap 5 templates, form helpers etc. so you can focus on the important stuff.    
It is *plain scala code* as a "template engine", so there is no separate language you need to learn.  

---

Let's make a simple HTML page that greets the user.  
Create a file `html.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.{*, given}

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(IndexView)
  case GET -> Path("hello", name) =>
    Response.withBody(HelloView(name))

UndertowSharafServer("localhost", 8181, routes).start()

println(s"Server started at http://localhost:8181")


object IndexView extends HtmlPage:
  override def pageContent = div(
    p("Welcome!"),
    a(href := "/hello/Bob")("Hello world")
  )

class HelloView(name: String) extends HtmlPage:
  override def pageContent =
    div("Hello ", b(name), "!")

```

and run it like this:
```sh
scala-cli html.sc 
```

Go to [http://localhost:8181](http://localhost:8181)  
to see how it works.

