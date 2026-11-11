# Sharaf Adoption Analysis

Sharaf is not mainly missing more examples. It is missing a clearly blessed, production-ready path.

The framework already has a broad surface: multiple servers, JSON/forms/validation, sessions, static files, HTMX,
SSE, native support, and substantial pac4j coverage. The larger gap is converting those pieces into confidence and a
five-minute path to a real application.

## Highest-priority gaps

### 1. One official starter

This would likely have more impact than another ten isolated examples.

Create one first-class starter, owned and tested in the main repository:

```text
my-sharaf-app/
  src/
    Main.scala
    config.scala
    users/
  test/
  resources/
    application.conf
    logback.xml
  migrations/
  Dockerfile
  compose.yaml
  build config
  README.md
```

It should include:

- Undertow as the blessed default
- Postgres connection and migrations
- structured logging and request IDs
- health/readiness endpoints
- graceful shutdown
- configuration and secret validation
- integration testing
- Docker image
- optional sessions/login
- a deployment recipe

Offer three generated variants eventually:

- `api` — JSON, validation, OpenAPI, JWT
- `web` — HTML, forms, sessions, HTMX, CSRF
- `secure-web` — pac4j login/logout/OIDC plus persistent sessions

There is already a Giter8 template linked from the quickstart, but it feels like an external example rather than the
canonical front door. Rails succeeds partly because `rails new` produces a coherent application, while Play prominently
directs users to a seed project. [Rails documents generation through deployment](https://guides.rubyonrails.org/getting_started.html),
and [Play puts its seed template directly in the getting-started path](https://www.playframework.com/getting-started).

### 2. Fix the trust leaks before adding features

A newcomer currently encounters conflicting signals:

- The README dependency is `0.17.0`: [README.md](README.md#L12)
- Docs metadata is `0.18.0`: [project.yaml](docs/_data/project.yaml#L9)
- The repository has released `0.19.0`
- The pac4j module uses 6.5.3, while examples use 6.1.2 and the OAuth example still uses `undertow-pac4j` 6.0.0:
  [deder.pkl](deder.pkl#L258)
- The API-reference URL ends in `index.`: [project.yaml](docs/_data/project.yaml#L71)
- Scala CLI example compilation is explicitly allowed to fail in CI: [ci.yml](.github/workflows/ci.yml#L40)
- Helidon has public `???` implementations for cookies and forms:
  [HelidonSharafRequest.scala](sharaf-helidon/src/ba/sake/sharaf/helidon/HelidonSharafRequest.scala#L23)
- snunit form handling is also `???`:
  [SnunitSharafRequest.scala](sharaf-snunit/src/ba/sake/sharaf/snunit/SnunitSharafRequest.scala#L50)

These matter more than adding WebSockets or another server backend. Establish explicit support tiers:

- Stable: Undertow
- Beta: http4s/JDK server
- Experimental: Helidon/snunit/native, until complete

A narrow framework that works completely is much more compelling than a wide framework with runtime holes.

### 3. Make the security work visible and deployable

Security is currently one of Sharaf's strongest differentiators, but users mostly see it through scattered examples and
a short philosophy page.

Add a dedicated versioned security guide covering:

- form login
- OAuth2/OIDC
- JWT/API authentication
- authorization by role
- logout
- session fixation and regeneration
- persistent/distributed `SessionStore`
- cookie configuration behind HTTPS
- CSRF protection for browser forms
- security headers
- trusted proxies and forwarded headers
- secret handling
- request/body-size limits
- authentication integration testing

Also publish a concise "secure production defaults" page. Play explicitly markets secure defaults and documents
production configuration; that gives evaluators confidence before they read implementation details.
[Play security-default overview](https://www.playframework.com/documentation/3.0.x/Highlights26),
[Play production guide](https://www.playframework.com/documentation/3.0.x/Production).

A Redis or JDBC session-store module—or at least a complete tested recipe—is especially important. The default in-memory
implementation correctly warns that it is not suitable for restart-safe production use, but the user needs an obvious
next step.

### 4. Add a small production-baseline module

Something like `sharaf-production` or composable handlers for:

- access logging with redaction
- request/correlation IDs
- health and readiness
- Micrometer metrics
- OpenTelemetry tracing
- response compression
- timeouts and maximum request sizes
- trusted-proxy handling
- graceful startup/shutdown

Spring Boot's real advantage is not routing syntax; it is the confidence that health, metrics, configuration, and
deployment concerns have established answers. Its Actuator documentation treats these as core production capabilities.
[Spring Boot production-ready features](https://docs.spring.io/spring-boot/reference/actuator/index.html).

This should remain optional and modular—Sharaf should not imitate Spring's size.

### 5. Make OpenAPI a headline integration

This is unusually low-hanging fruit because OpenApi4s already exists.

A polished, documented combination could provide:

- OpenAPI-first Sharaf route generation
- generated request/response types
- Swagger UI
- generated clients
- compatibility tests ensuring implementation and specification agree

That gives Sharaf a much sharper API-framework story than "another pleasant router."

### 6. Complete the documentation journey

The current docs have many useful short pages, but not a continuous journey from blank directory to production.

A complete journey would cover:

1. Why Sharaf?
2. Create an app
3. Build one database-backed feature
4. Validate input and return errors
5. Add authentication and authorization
6. Test it
7. Observe it
8. Package and deploy it
9. Upgrade it

The quickstart currently lists build-tool dependency snippets and examples, but does not actually create and run a
complete project: [quickstart.md](docs/content/tutorials/quickstart.md#L7).

Also add:

- versioned documentation
- compatibility matrix: Sharaf × Scala × JDK × pac4j × server adapters
- migration guides for every breaking release
- module-selection guide
- complete API reference
- production checklist
- security policy and supported-version policy

## What not to prioritize yet

Defer:

- more HTTP server adapters
- a DI container
- ORM abstractions
- background-job framework
- mailers
- asset pipeline
- GraphQL
- WebSockets as a core abstraction

WebSockets can remain server-specific for now, especially because Sharaf already has SSE and a credible
HTMX/server-rendered niche. Http4s demonstrates the value of integrations and interchangeable backends, but Sharaf does
not need to duplicate its entire ecosystem. [Http4s integrations](https://http4s.org/v0.23/docs/integrations.html).

## Positioning matters more than parity

Trying to be "another Play/Spring/http4s" is probably unwinnable. Sharaf needs a sentence that makes the correct Scala
developer immediately interested.

One possible position is:

> Sharaf is the direct-style Scala 3 framework for secure, server-rendered and JSON applications—without effects,
> annotations, or a DI container.

Or, more narrowly:

> The Rails-like Scala 3 stack for Postgres, HTMX, and secure authentication.

That second position creates an opinionated stack:

```text
Sharaf + Undertow + Tupson + Validson + Squery/Postgres
       + HTMX/Twirl + pac4j + Testcontainers
```

This is more memorable than "minimalistic batteries-included framework," which is pleasant but generic.

## Suggested order

For the next two releases:

1. Remove support/runtime holes and make all advertised examples mandatory in CI.
2. Synchronize versions, repair API docs, and publish a compatibility matrix.
3. Turn the new pac4j integration into a complete security guide.
4. Ship one canonical production starter.
5. Add health, request logging/IDs, graceful shutdown, and metrics.
6. Integrate OpenApi4s and publish an API starter.
7. Produce one end-to-end tutorial and deploy it publicly.

Sharaf currently has about 65 stars and only two indexed dependents, so the adoption problem is still discoverability
and confidence, not framework scale. See the [current Scaladex listing](https://index.scala-lang.org/sake92/sharaf).

The best next milestone is therefore not "more batteries"; it is:

> Clone one starter, change the app name, and safely deploy it the same day.

That would make the existing feature set feel much larger than it currently does.
