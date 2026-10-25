---
layout: howto.html
title: Implement authorization
description: Implement authorization in Sharaf
---

# {{ page.title }}

Choose the smallest pattern that enforces your application's access rules. The following guides progress from
requiring a signed-in identity to checking attributes and relationships for a specific resource.

| Task | Guide |
|---|---|
| Restrict an endpoint to authenticated callers | [Require authentication](/howtos/authorization-authentication.html) |
| Allow any of several broad roles | [Require any role](/howtos/authorization-roles.html) |
| Query or mutate data belonging to the caller | [Enforce resource ownership](/howtos/authorization-ownership.html) |
| Combine account and resource attributes | [Implement an ABAC policy](/howtos/authorization-abac.html) |
| Enforce course or tenant membership, including combined roles | [Authorize scoped relationships](/howtos/authorization-relationships.html) |

## Run and adapt the examples

Install Scala CLI and a supported JDK, clone the Sharaf repository, and run the commands in each guide from the
repository root. Each example runs independently on `localhost:8181`; stop it with Ctrl-C before starting another.
All dependencies are pinned in the example file. No database or external identity provider is required.

Use the complete example linked in each guide as the starting point. The helpers `authenticated`,
`authorizedAnyRole`, and `withCurrentUser` are application code included in the examples; they are not Sharaf APIs.
`authenticated` decorates one `SharafHandler` at application construction time, with pac4j matchers for public exemptions.
`authorizedAnyRole` guards a matched route body when needed; `withCurrentUser` supplies an application identity through
a context function when queries or service policies need it. Both run inside the authenticated routes.

The examples use HTTP Basic authentication with explicit **development-only** accounts and password `demo`.
They use `NoOpSessionStore` and private in-memory data that resets on restart. For deployment, replace the demonstration
authenticator with your real identity mechanism, use HTTPS, and follow the [production security guide](/howtos/security.html).
Keep the service policies when changing login mechanisms. For a persistent store, check permissions and change data
within a suitable transaction or use a write constrained by both actor and resource identifiers.

## Preserve denial responses

Use 401 for missing or invalid API credentials and preserve the authentication challenge. Use 403 when an authenticated
caller lacks permission. Return 404 deliberately when private resource existence must be concealed.
Keep these responses consistent for reads and writes. Browser form/OIDC clients may redirect to login instead of
returning the direct client's API 401.

Map application exceptions **inside** the pac4j boundary so pac4j does not wrap the original exception before Sharaf
can map it. The ownership, ABAC, and relationship examples do this in `authenticated`:

```scala
Pac4jSecurityHandler(security, SharafHandler.exceptions(next, ExceptionMapper.json))
```

An outer exception handler can handle unexpected security or adapter errors. Do not unwrap arbitrary exception causes
to expose them to clients.

## Verify your policies through the API

Run all five HTTP integration suites without starting servers manually:

```sh
scala test examples/scala-cli/authorization --server=false
```

Each example has its own adjacent `.test.scala` file and independently runnable suite. Only the HTTP/server fixture
is shared. To test one example in isolation:

```sh
cd examples/scala-cli/authorization
scala test 04_abac.scala 04_abac.test.scala HttpExampleSuite.test.scala --server=false
```

The tests start the actual example applications on free ports and stop them afterward. They verify successful access,
missing and invalid credentials, any-role semantics, empty role sets, ownership, forged request attributes, independent
ABAC conditions, combined roles, cross-course access, and unchanged state after denied mutations.
Add equivalent denial cases whenever you add a policy or a new entry point.

For the reasoning behind these patterns, see [authorization models and context functions](/philosophy/authorization.html).
