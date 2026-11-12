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

Try a standalone example:

- [Scala CLI examples]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli) cover routes, JSON, forms, validation, and native executables.
- [HTMX examples]({{site.data.project.gh.sourcesUrl}}/examples/htmx) demonstrate interactive HTML.
- [Authorization examples]({{site.data.project.gh.sourcesUrl}}/examples/scala-cli/authorization) cover roles, ownership, and access policies.

Explore a larger application:

- [API example]({{site.data.project.gh.sourcesUrl}}/examples/api): JSON and validation.
- [Full-stack example]({{site.data.project.gh.sourcesUrl}}/examples/fullstack): HTML, static files, and forms.
- [OpenAPI API starter](https://github.com/sake92/sharaf-api-starter): generated routes and client, PostgreSQL, and integration tests.
- [Todo backend](https://github.com/sake92/sharaf-todo-backend): a TodoBackend implementation with CORS.
- [PetClinic](https://github.com/sake92/sharaf-petclinic): a full-stack application with PostgreSQL and integration tests.
- [Full-stack Giter8 template](https://github.com/sake92/sharaf-fullstack.g8): a starting point for a new application.

For specific integrations, see [form login]({{site.data.project.gh.sourcesUrl}}/examples/user-pass-form),
[JWT]({{site.data.project.gh.sourcesUrl}}/examples/jwt), and [OAuth2]({{site.data.project.gh.sourcesUrl}}/examples/oauth2)
with pac4j; or the [http4s]({{site.data.project.gh.sourcesUrl}}/examples/http4s) and
[snunit]({{site.data.project.gh.sourcesUrl}}/examples/snunit) server examples. See also the
[server reference](/reference/servers.html) for platform support and native examples.
