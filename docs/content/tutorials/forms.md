---
title: Forms
description: Sharaf Tutorial Forms
---

# {{ page.title }}

Form data can be extracted with `Request.current.bodyForm[MyData]`.  
The `MyData` needs to have a `FormDataRW` given instance.

Create a file `form_handling.sc` and paste this code into it:

```scala
{% include 'form_handling.sc' %}
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
