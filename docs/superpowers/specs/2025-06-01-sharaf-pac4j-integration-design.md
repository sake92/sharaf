# Sharaf-pac4j Integration Design

## Overview

Rebuild `sharaf-pac4j` as a clean adapter layer between Sharaf's HTTP/session abstractions and pac4j's security framework. Follow play-pac4j's proven architecture: three separate adapter classes (WebContext, SessionStore, HttpActionAdapter), a config builder, and handler decorators for security/callback/logout. Also restructure Sharaf's session types into a dedicated `ba.sake.sharaf.session` package.

---

## Phase 1: Session Package Restructuring

### 1.1 New Package: `ba.sake.sharaf.session`

Move session types out of the crowded `ba.sake.sharaf` root package and the `handlers` sub-package into a dedicated `ba.sake.sharaf.session` package.

### 1.2 File Changes

#### Moves and renames

| Source | Destination | Changes |
|--------|-------------|---------|
| `src/ba/sake/sharaf/Session.scala` | `src/ba/sake/sharaf/session/Session.scala` | Move only; update `SessionImpl` references (was `SharafSession`) |
| `src/ba/sake/sharaf/SharafSession.scala` | `src/ba/sake/sharaf/session/SessionImpl.scala` | Rename class `SharafSession` → `SessionImpl` |
| `src/ba/sake/sharaf/SessionConfig.scala` | `src/ba/sake/sharaf/session/SessionConfig.scala` | Move only |
| `src/ba/sake/sharaf/SessionStore.scala` | `src/ba/sake/sharaf/session/SessionStore.scala` | Simplify: remove `cookieValue`, rename `load` param to `sessionId` |
| `src/ba/sake/sharaf/handlers/SessionHandler.scala` | `src/ba/sake/sharaf/session/SessionHandler.scala` | Move; use `session.id` directly for cookie value |
| `src-jvm/ba/sake/sharaf/SecureSessionId.scala` | `src-jvm/ba/sake/sharaf/session/SecureSessionId.scala` | Move; update `private[sharaf]` → `private[sharaf.session]` |
| `src-jvm/ba/sake/sharaf/SessionHolder.scala` | `src-jvm/ba/sake/sharaf/session/SessionHolder.scala` | Move; update visibility |
| `src-jvm/ba/sake/sharaf/InMemorySessionStore.scala` | `src-jvm/ba/sake/sharaf/session/InMemorySessionStore.scala` | Move; update imports to new package |
| `src-native/ba/sake/sharaf/SecureSessionId.scala` | `src-native/ba/sake/sharaf/session/SecureSessionId.scala` | Same as JVM |
| `src-native/ba/sake/sharaf/SessionHolder.scala` | `src-native/ba/sake/sharaf/session/SessionHolder.scala` | Same as JVM |
| `src-native/ba/sake/sharaf/InMemorySessionStore.scala` | `src-native/ba/sake/sharaf/session/InMemorySessionStore.scala` | Same as JVM |

#### Deletions

| File | Reason |
|------|--------|
| `src-jvm/ba/sake/sharaf/CookieSessionStore.scala` | Removed — simplified model where cookie always stores session ID. Client-side signed sessions are JWT's domain. |

#### Additions

| File | Description |
|------|-------------|
| `src/ba/sake/sharaf/session/NoOpSessionStore.scala` | Ephemeral session store: creates sessions with generated IDs, never persists, `load` returns `None` |

### 1.3 Simplified `SessionStore` Trait

```scala
trait SessionStore:
  def create(): SessionImpl
  def load(sessionId: String): Option[SessionImpl]
  def save(session: SessionImpl): Unit
  def delete(sessionId: String): Unit
```

**Removed**: `cookieValue(session: SessionImpl): String` — no longer needed. `SessionHandler` always uses `session.id` as the cookie value.

### 1.4 `SessionImpl` (renamed from `SharafSession`)

Same implementation as before — mutable `var`-based session with typed `get`/`set` via Tupson JSON. Package `ba.sake.sharaf.session`. Constructor uses `private[sharaf.session]` visibility.

### 1.5 `NoOpSessionStore`

```scala
final class NoOpSessionStore extends SessionStore:
  override def create(): SessionImpl =
    new SessionImpl(SecureSessionId.generate(), Instant.now(), Instant.now(), Map.empty)
  override def load(sessionId: String): Option[SessionImpl] = None
  override def save(session: SessionImpl): Unit = ()
  override def delete(sessionId: String): Unit = ()

object NoOpSessionStore:
  val instance: NoOpSessionStore = new NoOpSessionStore
```

No cookie is ever set because `Pac4jSecurityHandler` (see Phase 2) skips cookie logic when this store is used.

### 1.6 `SessionHandler` Update

`SessionHandler` simplifies since the cookie value is always `session.id`:

```scala
final class SessionHandler(store: SessionStore, config: SessionConfig, next: SharafHandler)
    extends SharafHandler:
  override def handle(context: RequestContext): Response[?] =
    val sessionId = context.request.cookies.find(_.name == config.cookieName).map(_.value)
    val session = sessionId.flatMap(store.load).getOrElse(store.create())
    session._lastAccessedAt = Instant.now()
    SessionHolder.set(session)
    val res = try next.handle(context) finally SessionHolder.clear()
    if session._invalidated then
      store.delete(session.id)
      res.removingCookie(config.cookieName)
    else
      if session._regenerated then session._previousId.foreach(store.delete)
      store.save(session)
      res.settingCookie(Cookie(
        name = config.cookieName,
        value = session.id,               // always the ID
        path = Some(config.cookiePath),
        maxAge = config.maxAge.map(_.getSeconds.toInt),
        secure = config.secure,
        httpOnly = config.httpOnly,
        sameSite = true,
        sameSiteMode = Some(config.sameSite)
      ))
```

### 1.7 Package Object Re-exports

In `ba.sake.sharaf.package` (cross-platform `src/`):

```scala
export ba.sake.sharaf.session.{
  Session, SessionConfig, SessionStore, SessionImpl,
  InMemorySessionStore, NoOpSessionStore
}
```

Existing code using `import ba.sake.sharaf.Session` continues to compile.

### 1.8 `SessionHolder` Visibility

Current: `private[sharaf]`. Update to `private[sharaf.session]` since it moves to the session package. `Session.current` is public and remains accessible — this is all sharaf-pac4j needs.

---

## Phase 2: sharaf-pac4j Adapter Layer

### 2.1 Package: `ba.sake.sharaf.pac4j`

All new files live here. Module depends on `sharaf-core` and `pac4j-core`.

### 2.2 `SharafWebContext` — pac4j `WebContext` Adapter

**Implements**: `org.pac4j.core.context.WebContext`

Wraps a Sharaf `Request`. Accumulates response modifications (cookies, headers, content type) in mutable collections. Provides `supplementResponse(res: Response[?]): Response[?]` to merge modifications back into a Sharaf `Response`.

**Methods**:

| pac4j method | Sharaf mapping |
|-------------|----------------|
| `getRequestURL()` | Build from request scheme + host header + path + query |
| `getRequestMethod()` | `request.method.name` |
| `getRequestCookies()` | Convert Sharaf `Cookie` → pac4j `Cookie`. MaxAge: `None` → `-1` (session) |
| `getRequestHeaders()` | `request.headers` flattened to `Map[String, String]` |
| `getRequestParameters()` | From `request.queryParamsRaw` + `request.bodyFormRaw` |
| `addResponseCookie(cookie)` | Convert pac4j `Cookie` → Sharaf `Cookie` (maxAge -1 → `None`, maxAge 0 → `expires=EPOCH`, maxAge>0 → `Some(seconds)`). Accumulate. |
| `setResponseHeader(name, value)` | Accumulate in mutable `Map` |
| `setResponseContentType(type)` | Accumulate as a header |
| `supplementResponse(res)` | Apply all accumulated cookies, headers, and content-type to a copy of `res` |
| `getRequestAttribute(key)` | Look up in internal mutable `HashMap[String, AnyRef]` |
| `setRequestAttribute(key, value)` | Store in internal mutable `HashMap` |
| `getNativeRequest()` | Returns the Sharaf `Request` |

**Cookie conversion detail**:

```
pac4j Cookie (seconds-based maxAge):
  -1       → Sharaf maxAge = None     (session cookie)
   0       → Sharaf expires = EPOCH   (delete cookie)  
  N > 0    → Sharaf maxAge = Some(N)

Sharaf Cookie (Option-based maxAge):
  None         → pac4j maxAge = -1
  expires=EPOCH → pac4j maxAge = 0
  Some(N)      → pac4j maxAge = N
```

**Factory**: companion object provides `WebContextFactory` for pac4j `Config`.

### 2.3 `SharafSessionStore` — pac4j `SessionStore` Adapter

**Implements**: `org.pac4j.core.context.session.SessionStore`

Wraps a Sharaf `SessionStore`. Bridges pac4j's key-value `Object` storage to Sharaf's typed JSON session.

**Internal mechanism**: Java serialization → Base64 → stored as `String` in Sharaf `Session` under `"pac4j."` prefixed keys. This is the same approach as play-pac4j's default `JavaSerializer`.

**Methods**:

| Method | Behavior |
|--------|----------|
| `getSessionId(context, createSession)` | Returns `session.id` from `Session.current`. If `createSession`, creates via Sharaf `SessionStore.create()` and sets `SessionHolder`. |
| `get(context, key)` | Reads from `Session.current` → deserializes Base64+Java → returns `Optional[AnyRef]` |
| `set(context, key, value)` | Java-serializes `value` → Base64 → stores in `Session.current` with key `"pac4j.$key"` |
| `destroySession(context)` | Calls `store.delete(sessionId)`, clears `SessionHolder` |
| `renewSession(context)` | Calls `session.regenerate()`, deletes old session ID from store |
| `getTrackableSession(context)` | Returns `Optional.of(sessionId)` for distributed session support |

**Factory**: companion object provides `SessionStoreFactory` for pac4j `Config`, accepting a Sharaf `SessionStore`.

### 2.4 `SharafHttpActionAdapter` — pac4j `HttpActionAdapter`

**Implements**: `org.pac4j.core.http.adapter.HttpActionAdapter`

Converts pac4j HTTP actions to Sharaf `Response` objects.

**Mappings**:

| pac4j Action | Sharaf Response |
|-------------|-----------------|
| `ForbiddenAction(403)` | `Response.withStatus(StatusCode.Forbidden)` |
| `UnauthorizedAction(401)` | `Response.withStatus(StatusCode.Unauthorized)` + `WWW-Authenticate` header if present |
| `OkAction(content)` | `Response.withStatus(StatusCode.Ok).withBody(content)` |
| `NoContentAction(204)` | `Response.withStatus(StatusCode.NoContent)` |
| `RedirectAction(location)` | `Response.redirect(location)` (default 302, or 303 if POST) |
| `BadRequestAction(400)` | `Response.withStatus(StatusCode.BadRequest)` |

Uses `SharafWebContext.supplementResponse()` via the context passed in the adapt call, so any accumulated cookies/headers are included.

### 2.5 `Pac4jSecurityConfig` — Wiring Config

Builder that creates a pac4j `Config` with all Sharaf factories pre-wired.

```scala
final class Pac4jSecurityConfig private (
    securityLogic: SecurityLogic,
    callbackLogic: CallbackLogic,
    logoutLogic: LogoutLogic,
    webContextFactory: WebContextFactory,
    sessionStoreFactory: SessionStoreFactory,
    httpActionAdapter: HttpActionAdapter,
    clients: Clients,
    authorizers: Authorizers,
    matchers: Matchers,
    sessionStore: ba.sake.sharaf.session.SessionStore,
    callbackUrl: Option[String],
    logoutUrl: Option[String]
):
  def toPac4jConfig: org.pac4j.core.config.Config = ...
  // withXxx builder methods for customization

object Pac4jSecurityConfig:
  def apply(
      clients: Clients,
      authorizers: Authorizers = Authorizers(),
      matchers: Matchers = Matchers(),
      sessionStore: ba.sake.sharaf.session.SessionStore = InMemorySessionStore(),
      callbackUrl: Option[String] = None,
      logoutUrl: Option[String] = None,
      securityLogic: SecurityLogic = DefaultSecurityLogic(),
      callbackLogic: CallbackLogic = DefaultCallbackLogic(),
      logoutLogic: LogoutLogic = DefaultLogoutLogic(),
  ): Pac4jSecurityConfig
```

### 2.6 `Pac4jSecurityHandler` — Security Handler Decorator

**Extends**: `SharafHandler`

Subsumes both session management and pac4j security in one handler. No separate `SessionHandler` needed when using pac4j.

**Constructor**:

```scala
final class Pac4jSecurityHandler(
    securityConfig: Pac4jSecurityConfig,
    clients: Option[String] = None,
    authorizers: Option[String] = None,
    matchers: Option[String] = None,
    next: SharafHandler
) extends SharafHandler
```

**Flow**:

```
handle(context):
  1. Create SharafWebContext from context.request
  2. Load/create session via SessionStore
  3. Set SessionHolder (→ Session.current works in wrapped routes)
  4. Create FrameworkParameters from context
  5. Call securityConfig.toPac4jConfig.getSecurityLogic.perform(...)
  6. On auth success:
     a. Call next.handle(context)
     b. webContext.supplementResponse(result)
     c. Save session (if not NoOpSessionStore), set session cookie
     d. Clear SessionHolder
     e. Return supplemented response
  7. On auth required (redirect/error):
     a. actionAdapter.adapt(action, webContext)
     b. webContext.supplementResponse(actionResult)
     c. Clear SessionHolder
     d. Return supplemented response
```

**Cookie setting**: `Pac4jSecurityHandler` sets the Sharaf session cookie (value = `session.id`) unless the store is `NoOpSessionStore`, in which case no cookie is set.

### 2.7 `Pac4jCallbackHandler` — OAuth/OIDC Callback Handler

**Extends**: `SharafHandler`

Handles the callback endpoint for OAuth/OIDC flows. Creates `SharafWebContext`, invokes pac4j's `CallbackLogic`, extracts the response, supplements it, and returns.

```scala
final class Pac4jCallbackHandler(securityConfig: Pac4jSecurityConfig) extends SharafHandler
```

### 2.8 `Pac4jLogoutHandler` — Logout Handler

**Extends**: `SharafHandler`

Handles local + central logout. Creates `SharafWebContext`, invokes pac4j's `LogoutLogic`, extracts the response, supplements it, and returns.

```scala
final class Pac4jLogoutHandler(securityConfig: Pac4jSecurityConfig) extends SharafHandler
```

---

## Phase 3: Handler Composition Patterns

### 3.1 Stateful (OAuth/OIDC with server-side sessions)

```scala
val pac4jConfig = Pac4jSecurityConfig(
  clients = Clients(OAuth20Client(...)),
  sessionStore = InMemorySessionStore(),
  callbackUrl = Some("/callback"),
  logoutUrl = Some("/")
)

val handler =
  CorsHandler(CorsSettings.default,
    ExceptionHandler(ExceptionMapper.json,
      RoutesHandler(Routes {
        case GET  -> Path()                => Response.withBody("public")
        case GET  -> Path("private")       => Pac4jSecurityHandler(pac4jConfig, 
                                               clients = Some("OAuthClient"), 
                                               next = routesForPrivate)
        case GET  -> Path("callback")      => Pac4jCallbackHandler(pac4jConfig)
        case GET  -> Path("logout")        => Pac4jLogoutHandler(pac4jConfig)
      }, notFound)
    )
  )
```

`Session.current` works inside routes. Session cookie is set. Session is persisted in `InMemorySessionStore`.

### 3.2 Stateless (JWT)

```scala
val pac4jConfig = Pac4jSecurityConfig(
  clients = Clients(HeaderClient("Authorization", JwtAuthenticator(...))),
  sessionStore = NoOpSessionStore.instance
)

val handler =
  CorsHandler(CorsSettings.default,
    ExceptionHandler(ExceptionMapper.json,
      RoutesHandler(Routes {
        case GET -> Path()           => Response.withBody("public")
        case GET -> Path("private")  => Pac4jSecurityHandler(pac4jConfig,
                                         clients = Some("HeaderClient"),
                                         next = routesForPrivate)
      }, notFound)
    )
  )
```

No session cookie. No server-side storage. pac4j decodes JWT on each request, `Session.current` returns an ephemeral session that exists only for the request duration.

### 3.3 Non-pac4j routes with sessions

For routes that need sessions but NOT pac4j auth, the standalone `SessionHandler` remains available:

```scala
val handler =
  SessionHandler(InMemorySessionStore(), SessionConfig.default,
    RoutesHandler(routes, notFound)
  )
```

---

## Phase 4: Build Configuration

In `deder.pkl`, the `sharafPac4jModules` section already exists:

```pkl
template = (baseTemplate) {
  pomSettings = pomSettings("sharaf-pac4j", "Sharaf Pac4j", "Sharaf integration with Pac4j security")
  moduleDeps { sharafCoreModules.jvm }
  deps { "org.pac4j:pac4j-http:6.5.2" /* moved from test */ }
}
testTemplate = (baseTemplate.asTest()) {
  moduleDeps {
    sharafCoreModules.jvm_test
    sharafJdkHttpserverModules.main
  }
  deps {
    "org.scalameta::munit::1.1.0"
    "org.pac4j:pac4j-http:6.5.2"
  }
}
```

Change `pac4j-http` from a test dep to a main dep (it provides `HeaderClient` used in JWT flows). Keep it in both `deps` (main) and `testTemplate.deps` (test uses `HeaderClient` directly).

---

## Phase 5: Testing

### 5.1 Core Changes

| Test | File | What it tests |
|------|------|---------------|
| `NoOpSessionStoreTest` | `sharaf-core/test/src/ba/sake/sharaf/session/NoOpSessionStoreTest.scala` (NEW) | `create()` returns valid session, `load()` returns `None`, `save`/`delete` are no-ops |
| `SessionHandlerTest` | Update existing tests in `sharaf-undertow/test/` and `sharaf-http4s/test/` for new package + simplified API |

### 5.2 Pac4j Tests

| Test | File | What it tests |
|------|------|---------------|
| `SharafWebContextTest` | `sharaf-pac4j/test/` (NEW) | Cookie conversion (both directions), header accumulation, `supplementResponse` merges all modifications |
| `SharafSessionStoreTest` | `sharaf-pac4j/test/` (NEW) | pac4j get/set roundtrips through Sharaf Session, destroySession, renewSession, prefix isolation |
| `SharafHttpActionAdapterTest` | `sharaf-pac4j/test/` (NEW) | Each pac4j action maps to correct Sharaf status/headers |
| `Pac4jSecurityHandlerTest` | `sharaf-pac4j/test/` (NEW) | Public pass-through, protected returns 401 without auth, protected returns 200 with auth, session cookie set, NoOp skips cookie |
| `Pac4jCallbackHandlerTest` | `sharaf-pac4j/test/` (NEW) | OAuth callback flow with mock client |

Test infrastructure: Use `JdkHttpServerSharafServer` for lightweight integration testing (same pattern as old tests).

### 5.3 Example App

Update `examples/jwt/src/Main.scala` to use the new API. The current example references `NoOpSessionStore.factory` which doesn't exist yet — this is the goal.

---

## Phase 6: Migration Impact

### Breaking Changes

| Change | Impact | Mitigation |
|--------|--------|------------|
| `SharafSession` → `SessionImpl` | Direct references break | Rare — users reference `Session` trait, not the impl |
| `CookieSessionStore` removed | Users of cookie-based sessions lose storage | Recommend JWT via pac4j for client-side auth |
| `InMemorySessionStore` moves package | Import changes needed | Re-exported from `ba.sake.sharaf` package object |
| `SessionStore.cookieValue` removed | Any custom `SessionStore` implementations break | Method was rarely overridden; default was `session.id` |
| `SessionHandler` moves package | Import changes needed | Re-exported from `ba.sake.sharaf` package object |

### Non-Breaking

| Change | Impact |
|--------|--------|
| `Session` trait stays public | No change for users |
| `Session.current` stays public | No change for users |
| `SessionConfig` API unchanged | No change for users |
| `SharafHandler.sessions()` factory unchanged | No change for users |

---

## Implementation Order

1. Move + rename session files to `ba.sake.sharaf.session`
2. Add `NoOpSessionStore` to core
3. Remove `CookieSessionStore`
4. Simplify `SessionStore` trait (remove `cookieValue`)
5. Update `SessionHandler` (use `session.id`)
6. Add package object re-exports
7. Update all references across sharaf-core modules (InMemorySessionStore imports, etc.)
8. Update server integration modules (Undertow, Http4s, Helidon, etc.)
9. Run core tests — ensure nothing broken
10. Build sharaf-pac4j adapter classes
11. Wire `Pac4jSecurityConfig` 
12. Build `Pac4jSecurityHandler`, `Pac4jCallbackHandler`, `Pac4jLogoutHandler`
13. Write pac4j tests
14. Update `examples/jwt` to use new API
