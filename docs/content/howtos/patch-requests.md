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

Import `ba.sake.sharaf.Patch` from core. Its JSON codec defaults a missing key to
`Keep`; every present key becomes `Set(value)`. Use
`Patch[String]` for a required field and `Patch[Option[String]]` for a nullable field, so JSON `null` becomes
`Set(None)`. Your request model can derive `JsonRW` as usual:

```scala
import ba.sake.sharaf.Patch
import ba.sake.tupson.JsonRW

case class UserPatch(name: Patch[String], address: Patch[Option[String]]) derives JsonRW
```

The runnable Scala CLI example applies the patch to a user resource:

<!-- example:start examples/scala-cli/patch_requests.sc -->
```scala
//> using scala 3.9.0
//> using dep ba.sake::sharaf-potateh:0.24.0

import ba.sake.sharaf.*
import ba.sake.sharaf.potateh.PotatehSharafServer
import ba.sake.tupson.{given, *}

case class User(name: String, address: Option[String]) derives JsonRW
case class UserPatch(name: Patch[String], address: Patch[Option[String]]) derives JsonRW

object Users:
  private var user = User("Grace", Some("Arlington"))

  def current: User = user

  def patch(update: UserPatch): User =
    user = User(
      name = update.name match
        case Patch.Set(value) => value
        case Patch.Keep       => user.name,
      address = update.address match
        case Patch.Set(value) => value
        case Patch.Keep       => user.address,
    )
    user

val routes = Routes:
  case GET -> Path("user") => Response.withBody(Users.current)

  case PATCH -> Path("user") =>
    Response.withBody(Users.patch(Request.current.bodyJson[UserPatch]))

PotatehSharafServer(routes, host = "localhost", port = 8181, exceptionMapper = ExceptionMapper.json).start()
println("Server started at http://localhost:8181")
```
<!-- example:end -->

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
