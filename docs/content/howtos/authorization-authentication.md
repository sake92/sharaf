---
layout: howto.html
title: Require authentication
description: Require authentication in Sharaf
---

# {{ page.title }}

Use this pattern when every authenticated caller may access an endpoint and its body does not need application-user data.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Protect the handler

Define an application `authenticated(next: SharafHandler)` helper that constructs a single `Pac4jSecurityHandler`
around all routes. Register a pac4j `PathMatcher` to exempt the exact landing path `/` and the public branch `/public`.
Keep pac4j's security-header matchers in the matcher list. Routes remain in one normal `Routes` definition; there is no
manual handler dispatch or duplicated path matching.

The example uses `excludePath("/")` for an exact path and `excludeBranch("/public")` for `/public` and its descendants.
`/public/help` is public, while `/publicity` still requires authentication. Path exemptions apply to every HTTP method;
only the explicitly defined route/method combinations return content.

If your application intentionally protects only a prefix, `includePath("/api/")` applies security to paths starting
with `/api/`; it does not cover `/api` itself, and other paths bypass security. Use explicit public exemptions when the
application should require authentication by default.

The `/members` route returns fixed content. It never calls `SecurityService.currentUser`, resolves a domain user, or
introduces a user context parameter. Authentication establishes access without requiring identity in the route body.

## Run and check access

```sh
scala run examples/scala-cli/authorization/01_authenticated.scala --server=false
```

In another terminal:

```sh
curl -i http://localhost:8181/                       # 200 Public
curl -i http://localhost:8181/public/help            # 200 Public help
curl -i http://localhost:8181/publicity              # 401: neighboring prefix
curl -i http://localhost:8181/members                # 401; WWW-Authenticate: Basic ...
curl -i -u alice:demo http://localhost:8181/members  # 200 Members only
curl -i -u alice:wrong http://localhost:8181/members # 401
```

`bob:demo` is also valid. No session cookie is created. An authenticated request to an unknown path receives 404;
an anonymous request to it still requires authentication.

If a route needs to query user-owned data, continue with [application identity and ownership](/howtos/authorization-ownership.html).
For a broad role requirement, use [authorizedAnyRole](/howtos/authorization-roles.html).


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 01_authenticated.scala 01_authenticated.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/01_authenticated.scala). The listing is synchronized from the tested source.

```scala
{% include "authorization-authenticated.scala" %}
```
