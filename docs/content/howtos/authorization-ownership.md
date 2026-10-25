---
layout: howto.html
title: Enforce resource ownership
description: Enforce resource ownership in Sharaf
---

# {{ page.title }}

Use this pattern when a route needs the caller's application identity and must protect a particular stored resource.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Resolve application identity at the boundary

After pac4j authenticates the request, map its trusted profile ID to the application's `CurrentUser`.
For multiple providers, include the provider/issuer in that mapping; do not assume IDs from different providers are unique.
Keep account lookup in this boundary code and keep pac4j profile types out of the service.

Use `withCurrentUser[A](action: CurrentUser ?=> A)` when several calls need the same identity. The example resolves the
user once per call and supplies it to service methods with `(using CurrentUser)`. Explicit user parameters are equally
appropriate for short call chains. Keep either form local to the operation, and resolve a fresh identity for each request.

A context parameter establishes which actor is available. The service must still check ownership for each resource.
A valid provider identity without a linked application account is denied with 403 in this example.

## Check stored ownership inside the service

Allow only the owner to read, rename, or delete a document. Read the owner from stored state and expose only `title`
in the rename request model. Do not accept owner changes as part of this operation.

The example uses one owner-checked read inside each synchronized write. In a database, use an appropriate transaction
or constrain the mutation by both document ID and owner ID, checking the result. Keep the service operation as the
enforcement boundary so REST, HTML, and future callers cannot bypass it.

Map `NotFoundException` to 404 for both a missing document and a private document belonging to someone else.
Place the application exception mapper [inside pac4j](/howtos/authorization.html#preserve-denial-responses).

## Run and check ownership

```sh
scala run examples/scala-cli/authorization/03_ownership.scala --server=false
```

```sh
curl -i -u alice:demo http://localhost:8181/documents/private # 200; Original, owned by alice
curl -i -u bob:demo http://localhost:8181/documents/private   # 404
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Stolen","ownerId":"bob"}' http://localhost:8181/documents/private # 404
curl -i -u alice:demo http://localhost:8181/documents/private # unchanged
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Renamed"}' http://localhost:8181/documents/private # 200; Renamed
curl -i -u bob:demo -X DELETE http://localhost:8181/documents/private   # 404
curl -i -u alice:demo -X DELETE http://localhost:8181/documents/private # 204
curl -i -u alice:demo http://localhost:8181/documents/private # 404 after deletion
```

`unlinked:demo` authenticates with pac4j but has no application account, so it receives 403.
Restart the server to reset the demonstration document.


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 03_ownership.scala 03_ownership.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/03_ownership.scala). The listing is synchronized from the tested source.

```scala
{% include "authorization-ownership.scala" %}
```
