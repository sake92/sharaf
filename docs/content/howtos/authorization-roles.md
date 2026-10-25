---
layout: howto.html
title: Require any role
description: Require any role in Sharaf
---

# {{ page.title }}

Use this pattern for a broad requirement such as allowing administrators **or** support staff to read a report.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Authenticate once, then guard the route body

Wrap the application's routes in one `Pac4jSecurityHandler`. Assign roles from trusted application state or verified
identity-provider claims during authentication. The example authenticator assigns `Admin`, `Support`, both roles, or
neither after validating the demonstration credentials.

Inside only the routes that need a role requirement, use the application helper:

```scala
case GET -> Path("reports") => authorizedAnyRole(Set(Role.Admin, Role.Support)) {
  Response.withBody("Report")
}
```

`authorizedAnyRole` reads the roles on the authenticated pac4j profiles. One matching role is enough; additional roles
do not prevent access. An empty allowed set grants nothing. Its action parameter is by-name, so denied requests never
execute the protected body. The helper throws `RejectedException` on denial, mapped to 403 inside the security handler.

Keep this guard inside authenticated routes: pac4j handles missing or invalid credentials with 401 before dispatch.
An authenticated request to an unknown route gets the ordinary 404; role guards run only after a route matches.
No application-user lookup, context function, second security handler, or manual handler dispatch is needed.

For a requirement shared by the entire protected handler, you can instead register pac4j's `RequireAnyRoleAuthorizer`
and configure its name through `Pac4jSecurityConfig.authorizers`. For an all-role requirement, use an explicitly named
all-role guard or `RequireAllRolesAuthorizer`; keep any-role and all-role semantics distinct. Pac4j's empty role list
has different semantics, so handle an empty allowed set explicitly when using its built-in authorizers.

## Run and check access

```sh
scala run examples/scala-cli/authorization/02_roles.scala --server=false
```

```sh
curl -i -u admin:demo http://localhost:8181/reports   # 200
curl -i -u support:demo http://localhost:8181/reports # 200
curl -i -u both:demo http://localhost:8181/reports    # 200
curl -i -u reader:demo http://localhost:8181/reports  # 403
curl -i http://localhost:8181/reports                # 401
curl -i -u admin:demo http://localhost:8181/closed    # 403: empty allowed set
curl -i http://localhost:8181/closed                 # 401
```

For a particular document, course, or tenant, add [ownership](/howtos/authorization-ownership.html),
[ABAC](/howtos/authorization-abac.html), or [relationship checks](/howtos/authorization-relationships.html)
in the service operation. A global role alone does not establish access to that resource.


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 02_roles.scala 02_roles.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/02_roles.scala). The listing is synchronized from the tested source.

```scala
{% include "authorization-roles.scala" %}
```
