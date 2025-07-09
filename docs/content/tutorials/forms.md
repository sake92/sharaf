---
title: Forms
description: Sharaf Tutorial Forms
---

# {{ page.title }}



Form data can be extracted with `Request.current.bodyForm[MyData]`.  
The `MyData` needs to have a `FormDataRW` given instance.

Create a file `form_handling.sc` and paste this code into it:

{# need to HTML encode these snippets, so that Markdown doesnt process them! #}
{% set contact_us_view = 'html"""
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
  """'
%}

```scala
//> using scala "3.7.0"
//> using dep {{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}

import ba.sake.formson.FormDataRW
import ba.sake.sharaf.{*, given}
import ba.sake.sharaf.undertow.UndertowSharafServer

val routes = Routes:
  case GET -> Path() =>
    Response.withBody(ContactUsView)
  case POST -> Path("handle-form") =>
    case class ContactUsForm(fullName: String, email: String) derives FormDataRW
    val formData = Request.current.bodyForm[ContactUsForm]
    Response.withBody(s"Got form data: ${formData}")

UndertowSharafServer("localhost", 8181, routes).start()

println("Server started at http://localhost:8181")

def ContactUsView =
  {{ contact_us_view | e }}
```

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
