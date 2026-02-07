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

## Named Tuples
You can also use named tuples to parse form params: `Request.current.bodyForm[(fullName: String, email: String)]`.
In this case you don't even need to define a separate class!

> Note that you can't use [Validation](/tutorials/validation.html) with named tuples

## Union Types
Union Types are also handy sometimes.
Say you have a filter functionality, where a user can submit a "firstName" or "lastName".
You can write: `Request.current.bodyForm[(firstName: String) | (lastName: String)]`.

Here we are combining 2 named tuples together with into a union type.
You could use any other type of course.


---

You can also write `Request.current.bodyForm[(id: Int | String)]`, but we would not recommend it.  
Of course, if you need it, go for it!

In this case, it will first try to parse an `Int` and if that fails it will parse a `String`.

{% include "tutorial_nav.html" %}
