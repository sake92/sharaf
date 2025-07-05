---
title: Forms
description: Sharaf Tutorial Forms
---

# {{ page.title }}



Form data can be extracted with `Request.current.bodyForm[MyData]`.  
The `MyData` needs to have a `FormDataRW` given instance.

Create a file `form_handling.sc` and paste this code into it:
```scala
//> using scala "3.7.0"
//> using dep ba.sake::sharaf-undertow:0.10.0

import scalatags.Text.all.*
import ba.sake.formson.FormDataRW
import ba.sake.sharaf.*
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(ContactUsView)
  case POST -> Path("handle-form") =>
    val formData = Request.current.bodyForm[ContactUsForm]
    Response.withBody(s"Got form data: ${formData}")

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")


def ContactUsView = doctype("html")(
  html(
    body(
      form(action := "/handle-form", method := "POST")(
        div(
          label("Full Name: ", input(name := "fullName", autofocus))
        ),
        div(
          label("Email: ", input(name := "email", tpe := "email"))
        ),
        input(tpe := "Submit")
      )
    )
  )
)

case class ContactUsForm(fullName: String, email: String) derives FormDataRW

```

Then run it like this:
```sh
scala-cli form_handling.sc 
```

Now go to [http://localhost:8181](http://localhost:8181)
and fill in the page with some data.

When you click the "Submit" button you will see a response like this:
```
Got form data: ContactUsForm(Bob,bob@example.com)
```
