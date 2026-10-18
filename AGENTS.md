# AGENTS.md

## Build System

This project uses **deder**, not sbt or mill. Config lives in `deder.pkl` (Pkl-based DSL).

```sh
# Install (once)
brew install sake92/tap/deder

# Common commands
deder exec -t compile                      # compile all modules
deder exec -t test                         # run all tests
deder exec -t test -m sharaf-undertow      # test single module
deder exec -t test -m sharaf-core -- --test=ba.sake.sharaf.routing.PathTest  # single test class
deder exec -t runMvnApp fmt                # format all sources with scalafmt
deder exec -t publishLocal                 # publish jars to local ~/.ivy2
```

## Architecture

- **Scala 3 only** — no Scala 2 support; use the version in `deder.pkl` (currently 3.9.0)
- **Multi-module monorepo** with ~10 library modules + ~10 example modules (see `deder.pkl`)
- **sharaf-core** is the central module. Server modules (undertow, http4s, helidon, jdk-httpserver, snunit) adapt it to specific HTTP servers. Companion libraries (validson, querson, formson) provide validation/parsing
- **Cross-platform**: JVM except snunit; Scala.js for querson/validson (formson JS disabled due to `java.nio.Path`); Scala Native for core, http4s, snunit, jdbc-session, formson/querson/validson. Check enabled modules and platform versions in `deder.pkl`; keep JVM APIs out of shared/Native sources.
- **Source layout** differs from standard sbt: `src/` (shared code), `src-jvm/` (JVM-only), `src-native/` (Native-only). Server modules follow this pattern too

## Testing

- Framework: **munit** (`munit.FunSuite`) — not ScalaTest
- Assertions use `assertEquals`, `assertNotEquals`, `test("name") { ... }`
- Integration tests use **sttp client4** (`sttp.client4.quick.*`) against locally-started servers
- Start the application's real Sharaf server/handler composition (e.g. `UndertowSharafServer`); use `NetworkUtils.getFreePort()` where supported and stop the server after testing. Reuse `sharaf-pac4j/test/` security helpers for auth tests.
- **Abstract test base classes** live in `sharaf-core/test/`. Each server module's tests extend these classes, providing a concrete server. Example: `AbstractSharafHandlerTest` in core → `SharafHandlerTest` in undertow/helidon/jdk-httpserver

## Code Conventions

- **Scalafmt 3.7.15**, Scala 3 dialect, max 120 chars per line. Format with `deder exec -t runMvnApp fmt`
- **Immutable builder pattern** for `Response`, `CorsSettings`, `Cookie` — use `.withStatus()`, `.settingHeader()`, `.settingCookie()` etc.
- **Context functions** for `Request` access: route handlers get `given Request`, use `Request.current` anywhere
- **Typeclass-based serialization**: use `derives JsonRW`, `derives QueryStringRW`, `derives FormDataRW` on case classes. Validation uses `derives Validator` from validson
- **JSON endpoints**: import `JsonRW` from `ba.sake.tupson`; use `Request.current.bodyJson[T]` and `Response.withBody(value)`. Configure `ExceptionMapper.json` for JSON errors. When derivation uses default values, enable `-Yretain-trees` in compiler options.
- **Existing core dependencies**: reuse Tupson for JSON, sttp client4 for outbound HTTP/tests, Formson for forms, Querson for queries, and Validson for validation. Avoid parallel JSON libraries or `java.net.http.HttpClient` unless a concrete requirement calls for them; select a platform-compatible sttp backend.
- **Forms and query parameters**: derive `FormDataRW` or `QueryStringRW` for typed parsing. Parsing alone does not validate input; use `bodyFormValidated[T]` or `queryParamsValidated[T]` with a `Validator` when needed. Multipart form support varies by server adapter.
- **Server adapters**: Undertow is stable; http4s and JDK HTTP server are beta; Helidon and snunit are experimental. Check platform and multipart support in `docs/content/reference/servers.md`.
- **Authentication (JVM)**: use `sharaf-pac4j` (`Pac4jSecurityConfig`, `Pac4jSecurityHandler`, `SecurityService`); add pac4j HTTP/JWT/OAuth/OIDC modules as needed. Follow `examples/user-pass-form/`, `examples/jwt/`, `examples/oauth2/`, and `sharaf-pac4j/test/`. Production browser auth needs HTTPS, CSRF protection, shared persistent sessions for replicas, and external secrets; stateless bearer APIs can use `NoOpSessionStore`.
- **JDBC sessions (JVM/Native)**: `sharaf-jdbc-session` supplies Squery-backed `JdbcSessionStore`; add a driver and any JVM pool, apply application-owned migrations from its schema templates, and schedule `deleteExpired()`. See `docs/content/howtos/security.md`.
- **HTML/UI**: JVM core includes Twirl (`ba.sake.sharaf.{*, given}`, `html` interpolation); optional `sharaf-hepek-components` adds Hepek `HtmlPage` responses via `ba.sake.sharaf.hepek.given`. Scalatags is separate. Reuse `examples/htmx/` for HTML fragments and the existing htmx version; see `examples/scala-cli/html*.sc` for rendering alternatives.
- **Config/logging**: Undertow includes Tupson Config/Typesafe Config (`docs/content/howtos/external-config.md`); other adapters need their own config dependencies (Helidon includes YAML config). Reuse existing logging; examples add Logback explicitly.
- **SQL/OpenAPI**: follow `docs/content/tutorials/sql.md` for Squery (optional outside JDBC sessions), and `docs/content/tutorials/openapi.md` for build-time OpenAPI4s with Sharaf/Tupson/Validson and optional sttp clients. Keep the application's build tool and generator off its runtime classpath.
- **Squery code generation**: prefer generated row models and DAOs where they fit over equivalent hand-written mappings/CRUD; generate against the migrated schema through the existing build integration or Squery CLI. See https://sake92.github.io/squery/tutorials/codegen.html.
- **Migrations**: when requested without a specified tool, default to Flyway; honor an explicit choice or established migration setup. Use application-owned, versioned migrations (including JDBC session tables), then regenerate Squery models/DAOs after schema changes.
- **Supporting dependencies**: `geny.Writable` response bodies are supported, including their content type. Fastparse, sourcecode, and scala-java-time serve parsing/derivation/platform needs; prefer Sharaf's public parsing APIs. jBCrypt is a form-login example dependency; pac4j-testkit/mock-oauth2-server, H2/Native SQLite, and jQuery are test/example dependencies. Consult `deder.pkl` for versions/scopes before adding libraries.
- **Route definitions** use pattern matching on `(HttpMethod, Path)` tuples: `case GET -> Path("hello", name) =>`

## Module → Package Map

| Directory | Scala package |
|---|---|
| `sharaf-core` | `ba.sake.sharaf` |
| `sharaf-undertow` | `ba.sake.sharaf.undertow` |
| `sharaf-http4s` | `ba.sake.sharaf.http4s` |
| `sharaf-helidon` | `ba.sake.sharaf.helidon` |
| `sharaf-jdk-httpserver` | `ba.sake.sharaf.jdkhttp` |
| `sharaf-snunit` | `ba.sake.sharaf.snunit` |
| `sharaf-hepek-components` | `ba.sake.sharaf.hepek` |
| `sharaf-pac4j` | `ba.sake.sharaf.pac4j` |
| `sharaf-jdbc-session` | `ba.sake.sharaf.session.jdbc` |
| `validson` | `ba.sake.validson` |
| `querson` | `ba.sake.querson` |
| `formson` | `ba.sake.formson` |

## Development Workflow

- **Worktrees**: Create isolated git worktrees in `.worktrees/` (already gitignored)
- **SPL/plan files**: Store and review specs/plans in `docs/superpowers/` but never commit them (also gitignored)
- PRs target `main`, merge when CI passes

## Release

- Tag-based release: `./scripts/release.sh <version>` — commits, tags, and pushes
- On tag push, CI runs tests then publishes non-example modules to Sonatype

## Docs

- Static site built with FlatMark, stored in `docs/`, served via GitHub Pages
- Sync example snippets into `docs/_includes/` with `scala docs/sync_examples.scala -- --write`; CI checks them with `scala docs/sync_examples.scala`
