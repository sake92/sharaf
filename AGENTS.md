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

- **Scala 3.7.3 only** — no Scala 2 support
- **Multi-module monorepo** with ~10 library modules + ~10 example modules (see `deder.pkl`)
- **sharaf-core** is the central module. Server modules (undertow, http4s, helidon, jdk-httpserver, snunit) adapt it to specific HTTP servers. Companion libraries (validson, querson, formson) provide validation/parsing
- **Cross-platform**: JVM (all), Scala.js (formson/querson/validson), Scala Native (core + http4s + snunit)
- **Source layout** differs from standard sbt: `src/` (shared code), `src-jvm/` (JVM-only), `src-native/` (Native-only). Server modules follow this pattern too

## Testing

- Framework: **munit** (`munit.FunSuite`) — not ScalaTest
- Assertions use `assertEquals`, `assertNotEquals`, `test("name") { ... }`
- Integration tests use **sttp client4** (`sttp.client4.quick.*`) against locally-started servers
- **Abstract test base classes** live in `sharaf-core/test/`. Each server module's tests extend these classes, providing a concrete server. Example: `AbstractSharafHandlerTest` in core → `SharafHandlerTest` in undertow/helidon/jdk-httpserver

## Code Conventions

- **Scalafmt 3.7.15**, Scala 3 dialect, max 120 chars per line. Format with `deder exec -t runMvnApp fmt`
- **Immutable builder pattern** for `Response`, `CorsSettings`, `Cookie` — use `.withStatus()`, `.settingHeader()`, `.settingCookie()` etc.
- **Context functions** for `Request` access: route handlers get `given Request`, use `Request.current` anywhere
- **Typeclass-based serialization**: use `derives JsonRW`, `derives QueryStringRW`, `derives FormDataRW` on case classes. Validation uses `derives Validator` from validson
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
- Example code snippets synced into `docs/_includes/` via `docs/copy-examples.ps1`
