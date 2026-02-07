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

```scala
{% include 'htmx_load_snippet.sc' %}
```

and run it like this:
```sh
scala htmx_load_snippet.sc 
```

Go to [http://localhost:8181](http://localhost:8181)  
to see how it works.

{% include "tutorial_nav.html" %}


