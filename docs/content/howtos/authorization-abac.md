---
layout: howto.html
title: Implement an ABAC policy
description: Implement an ABAC policy in Sharaf
---

# {{ page.title }}

Use attribute-based access control when permission depends on several facts about the actor and the resource.
Follow the [example prerequisites](/howtos/authorization.html#run-and-adapt-the-examples).

## Define a focused policy

For editing a document, require an active account, an active subscription, ownership, and an unarchived document:

```scala
def canEdit(user: CurrentUser, document: Document): Boolean =
  user.active && user.subscribed && document.ownerId == user.id && !document.archived
```

Resolve account attributes from trusted application state and load resource attributes from storage.
Do not take `active`, `subscribed`, `ownerId`, or `archived` from the edit request.
If your policy also depends on environment facts such as time, obtain them from a trusted source at the operation boundary.

In `Documents.rename`, call `EditPolicy.requireCanEdit(user, document)` before changing stored state. Use the same predicate for any displayed edit capability,
while keeping the service check decisive. Keep the check and write consistent through synchronization in this demo or
an appropriate database transaction/conditional write in an application.

The read policy is separate: an owner may inspect their document even when editing is unavailable.
This lets the API show that rejected edits did not change stored state.

## Run and check each condition

```sh
scala run examples/scala-cli/authorization/04_abac.scala --server=false
```

```sh
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Updated"}' http://localhost:8181/documents/draft # 200
curl -i -u bob:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/draft # 403: another owner
curl -i -u trial:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/trial # 403: no subscription
curl -i -u inactive:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/inactive # 403: inactive account
curl -i -u alice:demo -X PUT -H 'Content-Type: application/json' \
  -d '{"title":"Denied"}' http://localhost:8181/documents/archived # 403: archived
curl -i -u alice:demo http://localhost:8181/documents/archived # 200; still Archived
```

The API suite varies each decisive condition independently and sends forged attributes to verify they cannot grant access.
For decisions involving course membership, add [scoped relationship checks](/howtos/authorization-relationships.html).


## Run this example's integration tests

From the example folder, run the application source, its adjacent tests, and the shared HTTP fixture together:

```sh
cd examples/scala-cli/authorization
scala test 04_abac.scala 04_abac.test.scala HttpExampleSuite.test.scala --server=false
```

This starts and stops the real server on a free port. No other examples or manually started servers are needed.

## Complete runnable example

[Source]({{ site.data.project.gh.sourcesUrl }}/examples/scala-cli/authorization/04_abac.scala). The listing is synchronized from the tested source.

```scala
{% include "authorization-abac.scala" %}
```
