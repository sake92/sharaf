---
layout: tutorial.html
title: Forms
description: Sharaf Tutorial Forms
---

# {{ page.title }}

Form data can be extracted with `Request.current.bodyForm[MyData]`.  
The `MyData` needs to have a `FormDataRW` given instance.

Create a file `form_handling.sc` and paste this code into it:

<!-- example:start examples/scala-cli/form_handling.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.potateh.PotatehSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(ContactUsView)
  case POST -> Path("handle-form") =>
    case class ContactUsForm(fullName: String, email: String) derives FormDataRW
    val formData = Request.current.bodyForm[ContactUsForm]
    Response.withBody(s"Got form data: ${formData}")

PotatehSharafServer(routes, host = "localhost", port = 8181).start()

println("Server started at http://localhost:8181")

def ContactUsView =
  html"""
    <!DOCTYPE html>
    <html>
    <body>
        <form action="/handle-form" method="POST">
            <div>
                <label>Full Name: <input type="text" name="fullName" autofocus></label>
            </div>
            <div>
                <label>Email: <input type="email" name="email"></label>
            </div>
            <input type="submit" value="Submit">
        </form>
    </body>
    </html>
  """
```
<!-- example:end -->

Then run it like this:
```sh
scala form_handling.sc 
```

Now go to [http://localhost:8181](http://localhost:8181)
and fill in the page with some data.

When you click the "Submit" button you will see a response like this:
```
Got form data: ContactUsForm(Bob,bob@example.com)
```

The [request reference](/reference/request.html) lists the supported form parsing types.
