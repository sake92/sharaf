# Authorization examples

Run these from the repository root with Scala CLI and a supported JDK. Each example is self-contained and starts
Undertow on `localhost:8181`. Run one at a time; Ctrl-C stops it. There is no startup side effect when tests import an
example. Pinned directives are intentionally repeated so each file can be downloaded and run independently.

| Example | Tests | Concept | Development users (password `demo`) |
|---|---|---|---|
| `01_authenticated.scala` | [01_authenticated.test.scala](01_authenticated.test.scala) | One handler, public path matcher, no domain-user lookup | alice, bob |
| `02_roles.scala` | [02_roles.test.scala](02_roles.test.scala) | `authorizedAnyRole`, including empty-set denial | admin, support, both, reader |
| `03_ownership.scala` | [03_ownership.test.scala](03_ownership.test.scala) | Application identity, context functions, owner-checked reads/writes/deletes | alice, bob, unlinked |
| `04_abac.scala` | [04_abac.test.scala](04_abac.test.scala) | Active account, subscription, ownership, resource state | alice, bob, trial, inactive |
| `05_relationships.scala` | [05_relationships.test.scala](05_relationships.test.scala) | Resource-scoped memberships and combined teacher/student roles | alice, bob, outsider |

```sh
scala run examples/scala-cli/authorization/01_authenticated.scala --server=false
scala run examples/scala-cli/authorization/02_roles.scala --server=false
scala run examples/scala-cli/authorization/03_ownership.scala --server=false
scala run examples/scala-cli/authorization/04_abac.scala --server=false
scala run examples/scala-cli/authorization/05_relationships.scala --server=false
```

For the first example:

```sh
curl -i http://localhost:8181/                       # public 200
curl -i http://localhost:8181/members                # 401
curl -i -u alice:demo http://localhost:8181/members  # 200
```

Run all HTTP integration tests with no manually started server:

```sh
scala test examples/scala-cli/authorization --server=false
```

Each example has an adjacent test file with its own suite. Only the server lifecycle, HTTP request builder, and
typed response decoder are shared in `HttpExampleSuite.test.scala`. Each test creates fresh application state.

Run an example and its tests independently, without compiling the other examples:

```sh
cd examples/scala-cli/authorization
scala test 01_authenticated.scala 01_authenticated.test.scala HttpExampleSuite.test.scala --server=false
scala test 02_roles.scala 02_roles.test.scala HttpExampleSuite.test.scala --server=false
scala test 03_ownership.scala 03_ownership.test.scala HttpExampleSuite.test.scala --server=false
scala test 04_abac.scala 04_abac.test.scala HttpExampleSuite.test.scala --server=false
scala test 05_relationships.scala 05_relationships.test.scala HttpExampleSuite.test.scala --server=false
```

The suites own real Undertow servers on free ports and uses sttp client4. It checks positive and negative access,
forged fields, unchanged state after rejected mutations, and cross-course boundaries. State is reset for each scenario.
CI runs all five suites.

These are demonstration credentials and in-memory stores. Use HTTPS and your actual authentication/account store in
production. The helpers are application code, not new Sharaf APIs. One pac4j handler authenticates requests; matchers exempt public paths.
`authorizedAnyRole` checks roles inside matched route bodies when needed. `withCurrentUser` supplies application identity
only where required. Resource policies stay in service operations.

Practical guidance: [Authorization How-to](https://sake92.github.io/sharaf/howtos/authorization.html).
