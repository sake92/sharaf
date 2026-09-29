---
layout: tutorial.html
title: OpenAPI-first API
description: Generate Sharaf APIs and STTP clients from an OpenAPI contract with OpenApi4s
---

# {{ page.title }}

[OpenApi4s](https://github.com/sake92/openapi4s) turns an OpenAPI document into Scala 3 code. With the Sharaf backend
it generates Tupson request/response models and controller route stubs. The contract is the source of truth, not a
second description of the routes.

This tutorial uses the [sbt-openapi4s](https://github.com/sake92/sbt-openapi4s) plugin. It keeps OpenApi4s outside the
application's runtime classpath: it is a build-time generator, while the generated sources depend only on Sharaf,
Tupson, Validson, and optionally STTP.

## 1. Write the contract

Create `openapi/greeting.yaml`:

```yaml
openapi: 3.0.3
info:
  title: Greeting API
  version: 1.0.0
paths:
  /greetings/{name}:
    get:
      operationId: getGreeting
      tags: [greetings]
      parameters:
        - name: name
          in: path
          required: true
          schema:
            type: string
            minLength: 1
      responses:
        '200':
          description: A greeting
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Greeting'
components:
  schemas:
    Greeting:
      type: object
      required: [message]
      properties:
        message:
          type: string
```

## 2. Add the generator to sbt

Add the plugin in `project/plugins.sbt`:

```scala
addSbtPlugin("ba.sake" % "sbt-openapi4s" % "0.1.0")
```

Enable and configure the plugin in `build.sbt`:

```scala
import ba.sake.openapi4s.OpenApi4sPlugin
import ba.sake.openapi4s.OpenApi4sPlugin.autoImport.*

lazy val canonicalOpenApiFile = file("openapi/greeting.yaml")

lazy val api = project
  .in(file("modules/api"))
  .enablePlugins(OpenApi4sPlugin)
  .settings(
    scalaVersion := "3.9.0",
    libraryDependencies += "ba.sake" %% "sharaf-undertow" % "0.20.0",
    openApi4sPackage := "com.example.greetings",
    openApi4sFile := canonicalOpenApiFile,
    openApi4sModels := "tupson",
    openApi4sFramework := Some("sharaf"),
    openApi4sValidation := "validson",
    openApi4sVersion := "0.9.0"
  )
```

Generate the contracts whenever the specification changes. The generated sources go below
`modules/api/src/main/scala/com/example/greetings`:

```sh
sbt api/openApi4sGenerate
```

OpenApi4s generation is additive: review and commit the resulting source changes. Do not hand-maintain another model or
route definition alongside the canonical document.

OpenApi4s can also generate a typed STTP client from the same contract. Keep that client in its own module, with its own
model backend, as shown by the [API starter](https://github.com/sake92/sharaf-api-starter) and the
[sbt-openapi4s documentation](https://github.com/sake92/sbt-openapi4s).

### Deder and Mill

If your application already uses another build tool, use its integration instead of adding sbt just for code generation:

- [deder-plugins](https://github.com/sake92/deder-plugins) provides the Deder OpenApi4s plugin.
- [mill-openapi4s](https://github.com/sake92/mill-openapi4s) provides the Mill OpenApi4s plugin.

All three integrations invoke OpenApi4s with the same model, framework, validation, and client backend choices.

## 3. Implement generated controller routes

The command creates `models/`, `controllers/`, and `clients/` under `src/com/example/greetings`. The generated
controller initially returns `501 Not Implemented`; replace that stub with your application behavior while retaining its
generated route pattern and request parsing. For example, the generated `GreetingsController` route can return its
generated `Greeting` model:

```scala
package com.example.greetings.controllers

import ba.sake.sharaf.*
import com.example.greetings.models.Greeting

class GreetingsController {
  def routes = Routes {
    case GET -> Path("greetings", name) =>
      Response.withBody(Greeting(s"Hello, $name"))
  }
}
```

Wire it as you would any other Sharaf routes:

```scala
import ba.sake.sharaf.Routes
import ba.sake.sharaf.undertow.UndertowSharafServer
import com.example.greetings.controllers.GreetingsController

val routes = Routes.merge(Seq(GreetingsController().routes))
UndertowSharafServer("0.0.0.0", 8080, routes).start()
```

## 4. Serve the contract and Swagger UI

Copy the canonical contract to `modules/api/src/main/resources/public/openapi.yaml`. `UndertowSharafServer` serves
classpath resources from `public`, so the document is then available as `/openapi.yaml`. Add the Swagger UI WebJar to
the `api` project:

```scala
libraryDependencies += "org.webjars" % "swagger-ui" % "5.20.1"
```

Expose a `/swagger` route whose HTML loads the WebJar assets and points Swagger UI at `/openapi.yaml`. The
[API starter's Swagger controller](https://github.com/sake92/sharaf-api-starter/blob/main/modules/api/src/main/scala/com/example/petclinic/ui/SwaggerUIController.scala)
is the maintained copy-and-adapt implementation.

## 5. Test the contract through HTTP

Use the generated STTP client in an integration test against a running server. This proves that the generated types,
routes, and implementation still agree:

```scala
import munit.FunSuite
import sttp.client4.*
import sttp.model.StatusCode
import com.example.greetings.client.clients.GreetingsClient

class GreetingIntegrationTest extends FunSuite {
  private val backend = DefaultSyncBackend()

  test("the generated client matches the running API") {
    val response = GreetingsClient("http://127.0.0.1:8080").getGreeting("Ada").send(backend)
    assertEquals(response.code, StatusCode.Ok)
    assertEquals(response.body.map(_.message), Right("Hello, Ada"))
  }
}
```

Put this test in a separate integration-test project that depends on the generated client module and starts the API.

Also make a direct request to `/openapi.yaml` in that suite and assert its `openapi` version and an expected
`operationId`. Together, those checks catch a missing deployed contract as well as a route or payload that drifted from
the generated client.

For a complete runnable reference—including database migrations, Docker, Swagger UI, generated Sharaf server
contracts, a generated STTP client, and integration tests—see
[sharaf-api-starter](https://github.com/sake92/sharaf-api-starter).
