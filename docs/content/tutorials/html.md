---
title: HTML
description: Sharaf Tutorial HTML
---

# {{ page.title }}

You can make an HTML snippet by using the `html""` interpolator.
Then you return it directly in the `Response.withBody()`.

Let's make a simple HTML page that greets the user.  
Create a file `html.sc` and paste this code into it:

```scala
{% include 'html.sc' %}
```

and run it like this:
```sh
scala html.sc 
```

Go to http://localhost:8181  
to see how it works.

{% include "tutorial_nav.html" %}
