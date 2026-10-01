---
layout: howto.html
title: PATCH Requests
description: Apply partial JSON updates in a Sharaf API
---

# {{ page.title }}

A `PUT` request normally replaces a whole resource, but a client often needs to update just one field. With ordinary
optional fields, JSON cannot distinguish an omitted key (leave the existing value unchanged) from `null` (replace a
nullable value with no value). Model whether the field was sent separately from its nullability so a PATCH endpoint can
preserve that intent.

Define a `Patch[T]` codec whose default for a missing key is `Keep`; every present key becomes `Set(value)`. Use
`Patch[String]` for a required field and `Patch[Option[String]]` for a nullable field, so JSON `null` becomes
`Set(None)`. The runnable Scala CLI example below applies the patch to a user resource:

```scala
{% include "patch_requests.sc" %}
```

Start it from `examples/scala-cli`:

```sh
scala patch_requests.sc
```

To update only the name, omit `address`:

```sh
curl -X PATCH http://localhost:8181/user \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada"}'
```

To clear the address, send it as `null`:

```sh
curl -X PATCH http://localhost:8181/user \
  -H 'Content-Type: application/json' \
  -d '{"address":null}'
```

For the JSON-specific behavior and a reusable standalone codec, see Tupson’s
[PATCH requests guide](https://sake92.github.io/tupson/howtos/patch-requests.html).
