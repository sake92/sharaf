---
layout: tutorial.html
title: Quickstart
description: Sharaf Tutorial Quickstart
---

# {{ page.title }}

This lesson starts a Sharaf server and makes one request. You need JDK 21 and [Scala CLI](https://scala-cli.virtuslab.org/) installed.

Create a file named `hello.sc` with this content:

```scala
{% include "hello.sc" %}
```

Run it from the directory containing the file:

```sh
scala hello.sc
```

In another terminal, make a request:

```sh
curl http://localhost:8181/hello/Bob
```

The response is `Hello Bob`. Stop the server with Ctrl-C.

The route matches a GET request whose path begins with `hello` and captures the next segment as `name`. Continue with [path parameters](/tutorials/path-params.html), or see [dependency setup](/reference/dependencies.html) for Mill and sbt.

## More examples

- [Scala CLI examples]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli), standalone examples using Scala CLI
- [Scala CLI HTMX examples]({{site.data.project.gh.sourcesUrl}}/examples/htmx), standalone examples featuring HTMX
- [API example]({{site.data.project.gh.sourcesUrl}}/examples/api) featuring JSON and validation
- [full-stack example]({{site.data.project.gh.sourcesUrl}}/examples/fullstack) featuring HTML, static files and forms
- [sharaf-todo-backend](https://github.com/sake92/sharaf-todo-backend), implementation of the [todobackend.com](http://todobackend.com/) spec, featuring CORS handling
- [Username+Password form login]({{site.data.project.gh.sourcesUrl}}/examples/user-pass-form) with [Pac4J](https://www.pac4j.org/)
- [JWT auth]({{site.data.project.gh.sourcesUrl}}/examples/jwt) with [Pac4J](https://www.pac4j.org/)
- [OAuth2 login]({{site.data.project.gh.sourcesUrl}}/examples/oauth2) with [Pac4J](https://www.pac4j.org/)
- [Snunit]({{site.data.project.gh.sourcesUrl}}/examples/snunit) demo app
- [Http4s]({{site.data.project.gh.sourcesUrl}}/examples/http4s) demo app
- [PetClinic](https://github.com/sake92/sharaf-petclinic) implementation, featuring full-stack app with Postgres db, config, integration tests etc.
- [Giter8 template for fullstack app](https://github.com/sake92/sharaf-fullstack.g8)
