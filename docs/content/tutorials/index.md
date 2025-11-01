---
title: Tutorials
description: Sharaf Tutorials
pagination:
  enabled: false
---

# {{ page.title }}

{%

set tutorials = [
    { label: "Quickstart", url: "/tutorials/quickstart.html" },
    { label: "Hello World", url: "/tutorials/hello-world.html" },
    { label: "Path Params", url: "/tutorials/path-params.html" },
    { label: "Query Params", url: "/tutorials/query-params.html" },
    { label: "Static Files", url: "/tutorials/static-files.html" },
    { label: "HTML", url: "/tutorials/html.html" },
    { label: "Forms", url: "/tutorials/forms.html" },
    { label: "JSON", url: "/tutorials/json.html" },
    { label: "Validation", url: "/tutorials/validation.html" },
    { label: "SQL", url: "/tutorials/sql.html" },
    { label: "Tests", url: "/tutorials/tests.html" },
    { label: "HTMX", url: "/tutorials/htmx.html" },
    { label: "Server Sent Events", url: "/tutorials/sse.html" }
]

%}

{% for tut in tutorials %}- [{{ tut.label }}]({{ tut.url}})
{% endfor %}






