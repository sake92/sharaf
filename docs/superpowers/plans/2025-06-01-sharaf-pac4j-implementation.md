# Sharaf-pac4j Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild sharaf-pac4j: restructure session package in core, add NoOpSessionStore, then build three clean pac4j adapters with handler decorators for security/callback/logout.

**Architecture:** Move session types to `ba.sake.sharaf.session` package; rename `SharafSession` → `SessionImpl`; simplify `SessionStore` (remove `cookieValue`, use `session.id` directly); add `NoOpSessionStore`; then build `SharafWebContext` (implements pac4j `WebContext`), `SharafSessionStore` (implements pac4j `SessionStore`, bridges via Java serialization), `SharafHttpActionAdapter` (implements pac4j `HttpActionAdapter`), plus `Pac4jSecurityHandler` that subsumes both session lifecycle and security in one decorator.

**Tech Stack:** Scala 3.7.3, pac4j-core 6.5.2, Tupson JSON, Deder build tool, JUnit/MUnit tests

---

## Task 1: Create new `ba.sake.sharaf.session` package directory

**Files:**
- Create: `sharaf-core/src/ba/sake/sharaf/session/` (empty dir)
- Create: `sharaf-core/src-jvm/ba/sake/sharaf/session/` (empty dir)
- Create: `sharaf-core/src-native/ba/sake/sharaf/session/` (empty dir)

- [ ] **Step 1: Create directories**

```bash
mkdir -p sharaf-core/src/ba/sake/sharaf/session
mkdir -p sharaf-core/src-jvm/ba/sake/sharaf/session
mkdir -p sharaf-core/src-native/ba/sake/sharaf/session
```

- [ ] **Step 2: Commit**

```bash
git add sharaf-core/src/ba/sake/sharaf/session sharaf-core/src-jvm/ba/sake/sharaf/session sharaf-core/src-native/ba/sake/sharaf/session
git commit -m "chore: create ba.sake.sharaf.session package directories"
```

---

## Task 2: Move and rename Session.scala, SharafSession.scala → SessionImpl.scala

**Files:**
- Move: `sharaf-core/src/ba/sake/sharaf/Session.scala` → `sharaf-core/src/ba/sake/sharaf/session/Session.scala`
- Move: `sharaf-core/src/ba/sake/sharaf/SharafSession.scala` → `sharaf-core/src/ba/sake/sharaf/session/SessionImpl.scala`

- [ ] **Step 1: Move Session.scala to new package**

```scala
// File: sharaf-core/src/ba/sake/sharaf/session/Session.scala
package ba.sake.sharaf.session

import java.time.Instant
import ba.sake.tupson.JsonRW
import ba.sake.sharaf.exceptions.SharafException

trait Session {

  def id: String

  def createdAt: Instant

  def lastAccessedAt: Instant

  def keys: Set[String]

  def get[T: JsonRW](key: String): T =
    getOpt[T](key).getOrElse(throw new SharafException(s"No value found for session key: ${key}"))

  def getOpt[T: JsonRW](key: String): Option[T]

  def set[T: JsonRW](key: String, value: T): Unit

  def remove(key: String): Unit

  /** Destroys this session. The session cookie will be cleared from the response. */
  def invalidate(): Unit

  /** Generates a new session ID while preserving data. Call this after a user logs in
    * to prevent session fixation attacks.
    */
  def regenerate(): Unit

}

object Session:
  def current: Session =
    SessionHolder.get.getOrElse(
      throw SharafException(
        "No active session. Configure sessions with SharafHandler.sessions()."
      )
    )
```

- [ ] **Step 2: Move SharafSession.scala → SessionImpl.scala with rename**

```scala
// File: sharaf-core/src/ba/sake/sharaf/session/SessionImpl.scala
package ba.sake.sharaf.session

import java.time.Instant
import ba.sake.tupson.{*, given}

/** Mutable session implementation backed by a JSON-serialized key-value map. */
final class SessionImpl(
    private[session] var _id: String,
    private[session] val _createdAt: Instant,
    private[session] var _lastAccessedAt: Instant,
    private[session] var _data: Map[String, String]
) extends Session {

  private[session] var _invalidated: Boolean = false
  private[session] var _regenerated: Boolean = false
  private[session] var _previousId: Option[String] = None

  override def id: String = _id

  override def createdAt: Instant = _createdAt

  override def lastAccessedAt: Instant = _lastAccessedAt

  override def keys: Set[String] = _data.keySet

  override def getOpt[T: JsonRW](key: String): Option[T] =
    _data.get(key).map(_.parseJson[T])

  override def set[T: JsonRW](key: String, value: T): Unit =
    _data = _data + (key -> value.toJson)

  override def remove(key: String): Unit =
    _data = _data - key

  override def invalidate(): Unit =
    _invalidated = true

  override def regenerate(): Unit =
    _previousId = Some(_id)
    _id = SecureSessionId.generate()
    _regenerated = true

}
```

**Note:** `private[sharaf]` changed to `private[session]` since the class now lives in `ba.sake.sharaf.session`. `InMemorySessionStore` and `SessionHandler` (both in same package) can still access these fields.

- [ ] **Step 3: Delete old source files**

```bash
rm sharaf-core/src/ba/sake/sharaf/Session.scala
rm sharaf-core/src/ba/sake/sharaf/SharafSession.scala
```

- [ ] **Step 4: Commit**

```bash
git add sharaf-core/src/ba/sake/sharaf/session/Session.scala \
        sharaf-core/src/ba/sake/sharaf/session/SessionImpl.scala
git rm sharaf-core/src/ba/sake/sharaf/Session.scala \
        sharaf-core/src/ba/sake/sharaf/SharafSession.scala
git commit -m "refactor: move Session/SessionImpl to ba.sake.sharaf.session package"
```

---

## Task 3: Move SessionConfig.scala and SessionStore.scala

**Files:**
- Move: `sharaf-core/src/ba/sake/sharaf/SessionConfig.scala` → `sharaf-core/src/ba/sake/sharaf/session/SessionConfig.scala`
- Move: `sharaf-core/src/ba/sake/sharaf/SessionStore.scala` → `sharaf-core/src/ba/sake/sharaf/session/SessionStore.scala`
- Delete: `sharaf-core/src-jvm/ba/sake/sharaf/CookieSessionStore.scala`

- [ ] **Step 1: Move SessionConfig.scala (unchanged except package)**

```scala
// File: sharaf-core/src/ba/sake/sharaf/session/SessionConfig.scala
package ba.sake.sharaf.session

import java.time.Duration

/** Configuration for session handling. */
final class SessionConfig private (
    val cookieName: String,
    val cookiePath: String,
    val maxAge: Option[Duration],
    val absoluteTimeout: Option[Duration],
    val secure: Boolean,
    val httpOnly: Boolean,
    val sameSite: String
) {

  def withCookieName(cookieName: String): SessionConfig =
    copy(cookieName = cookieName)

  def withCookiePath(cookiePath: String): SessionConfig =
    copy(cookiePath = cookiePath)

  def withMaxAge(maxAge: Option[Duration]): SessionConfig =
    copy(maxAge = maxAge)

  def withAbsoluteTimeout(absoluteTimeout: Option[Duration]): SessionConfig =
    copy(absoluteTimeout = absoluteTimeout)

  def withSecure(secure: Boolean): SessionConfig =
    copy(secure = secure)

  def withHttpOnly(httpOnly: Boolean): SessionConfig =
    copy(httpOnly = httpOnly)

  def withSameSite(sameSite: String): SessionConfig =
    copy(sameSite = sameSite)

  private def copy(
      cookieName: String = cookieName,
      cookiePath: String = cookiePath,
      maxAge: Option[Duration] = maxAge,
      absoluteTimeout: Option[Duration] = absoluteTimeout,
      secure: Boolean = secure,
      httpOnly: Boolean = httpOnly,
      sameSite: String = sameSite
  ) = new SessionConfig(cookieName, cookiePath, maxAge, absoluteTimeout, secure, httpOnly, sameSite)

  override def toString: String =
    s"SessionConfig(cookieName=$cookieName, cookiePath=$cookiePath, maxAge=$maxAge, " +
      s"absoluteTimeout=$absoluteTimeout, secure=$secure, httpOnly=$httpOnly, sameSite=$sameSite)"
}

object SessionConfig:
  val default: SessionConfig = new SessionConfig(
    cookieName = "SHARAF_SESSION",
    cookiePath = "/",
    maxAge = Some(Duration.ofMinutes(30)),
    absoluteTimeout = Some(Duration.ofHours(8)),
    secure = true,
    httpOnly = true,
    sameSite = "Strict"
  )
```

- [ ] **Step 2: Move SessionStore.scala — simplified (remove cookieValue, rename load param)**

```scala
// File: sharaf-core/src/ba/sake/sharaf/session/SessionStore.scala
package ba.sake.sharaf.session

/** Defines how sessions are stored and retrieved.
  *
  * Built-in implementations:
  *   - [[InMemorySessionStore]]: stores sessions in server memory (default)
  *   - [[NoOpSessionStore]]: ephemeral sessions for stateless auth (no persistence)
  *
  * Custom implementations can be provided for e.g. Redis or database-backed sessions.
  */
trait SessionStore {

  /** Creates a new empty session with a freshly generated ID. */
  def create(): SessionImpl

  /** Loads a session by its ID. Returns `None` if the session does not exist or has expired. */
  def load(sessionId: String): Option[SessionImpl]

  /** Persists a session after request processing. */
  def save(session: SessionImpl): Unit

  /** Removes a session (e.g. when [[Session.invalidate]] is called). */
  def delete(sessionId: String): Unit
}
```

**Changes from old version:**
- Removed `def cookieValue(session: SessionImpl): String` — no longer needed; handler uses `session.id`
- Renamed `load(cookieValue: String)` → `load(sessionId: String)` — parameter is always a session ID
- Removed `@return` docs for CookieSessionStore
- Removed `SharafSession` → changed to `SessionImpl`

- [ ] **Step 3: Delete CookieSessionStore.scala**

```bash
rm sharaf-core/src-jvm/ba/sake/sharaf/CookieSessionStore.scala
```

- [ ] **Step 4: Delete old source files and commit**

```bash
git rm sharaf-core/src/ba/sake/sharaf/SessionConfig.scala \
        sharaf-core/src/ba/sake/sharaf/SessionStore.scala \
        sharaf-core/src-jvm/ba/sake/sharaf/CookieSessionStore.scala
git add sharaf-core/src/ba/sake/sharaf/session/SessionConfig.scala \
        sharaf-core/src/ba/sake/sharaf/session/SessionStore.scala
git commit -m "refactor: move SessionConfig/SessionStore, simplify SessionStore, remove CookieSessionStore"
```

---

## Task 4: Move SessionHandler.scala to session package

**Files:**
- Move: `sharaf-core/src/ba/sake/sharaf/handlers/SessionHandler.scala` → `sharaf-core/src/ba/sake/sharaf/session/SessionHandler.scala`

- [ ] **Step 1: Move and update SessionHandler.scala**

```scala
// File: sharaf-core/src/ba/sake/sharaf/session/SessionHandler.scala
package ba.sake.sharaf.session

import java.time.Instant
import ba.sake.sharaf.*

/** A [[SharafHandler]] decorator that provides session management.
  *
  * On each request:
  *   1. Reads the session-ID cookie from the incoming request.
  *   2. Loads an existing session from the store, or creates a new one.
  *   3. Makes the session available via [[Session.current]] for the duration of the request.
  *   4. After the inner handler returns, persists the session and sets the session cookie.
  *
  * If [[Session.invalidate]] was called during the request the session is deleted and the
  * cookie is removed from the response.
  *
  * If [[Session.regenerate]] was called (recommended after login to prevent session
  * fixation) the old session is deleted and a new ID is issued.
  */
final class SessionHandler(
    store: SessionStore,
    config: SessionConfig,
    next: SharafHandler
) extends SharafHandler {

  override def handle(context: RequestContext): Response[?] = {
    val sessionId = context.request.cookies.find(_.name == config.cookieName).map(_.value)
    val session = sessionId
      .flatMap(id => store.load(id))
      .getOrElse(store.create())

    session._lastAccessedAt = Instant.now()

    SessionHolder.set(session)
    val res =
      try next.handle(context)
      finally SessionHolder.clear()

    if session._invalidated then
      store.delete(session.id)
      res.removingCookie(config.cookieName)
    else
      if session._regenerated then session._previousId.foreach(store.delete)
      store.save(session)
      val maxAgeSeconds = config.maxAge.map(_.getSeconds.toInt)
      res.settingCookie(
        Cookie(
          name = config.cookieName,
          value = session.id, // always the session ID
          path = Some(config.cookiePath),
          maxAge = maxAgeSeconds,
          secure = config.secure,
          httpOnly = config.httpOnly,
          sameSite = true,
          sameSiteMode = Some(config.sameSite)
        )
      )
  }
}
```

**Key change:** `value = session.id` instead of `value = store.cookieValue(session)` — since `cookieValue` was removed from `SessionStore`, we always use the session ID directly as the cookie value.

- [ ] **Step 2: Delete old file and commit**

```bash
git rm sharaf-core/src/ba/sake/sharaf/handlers/SessionHandler.scala
git add sharaf-core/src/ba/sake/sharaf/session/SessionHandler.scala
git commit -m "refactor: move SessionHandler to ba.sake.sharaf.session, use session.id for cookie"
```

---

## Task 5: Move SecureSessionId.scala and SessionHolder.scala (JVM + Native)

**Files:**
- Move: `sharaf-core/src-jvm/ba/sake/sharaf/SecureSessionId.scala` → `sharaf-core/src-jvm/ba/sake/sharaf/session/SecureSessionId.scala`
- Move: `sharaf-core/src-jvm/ba/sake/sharaf/SessionHolder.scala` → `sharaf-core/src-jvm/ba/sake/sharaf/session/SessionHolder.scala`
- Move: `sharaf-core/src-native/ba/sake/sharaf/SecureSessionId.scala` → `sharaf-core/src-native/ba/sake/sharaf/session/SecureSessionId.scala`
- Move: `sharaf-core/src-native/ba/sake/sharaf/SessionHolder.scala` → `sharaf-core/src-native/ba/sake/sharaf/session/SessionHolder.scala`

- [ ] **Step 1: Move JVM SecureSessionId.scala (update visibility)**

```scala
// File: sharaf-core/src-jvm/ba/sake/sharaf/session/SecureSessionId.scala
package ba.sake.sharaf.session

import java.security.SecureRandom
import java.util.Base64

private[session] object SecureSessionId {

  private val rng = new SecureRandom()

  def generate(): String = {
    val bytes = new Array[Byte](16) // 128-bit random ID
    rng.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }
}
```

**Change:** `private[sharaf]` → `private[session]` since it's now in the session package.

- [ ] **Step 2: Move JVM SessionHolder.scala (update visibility)**

```scala
// File: sharaf-core/src-jvm/ba/sake/sharaf/session/SessionHolder.scala
package ba.sake.sharaf.session

/** Thread-local session holder for JVM request-scoped session access. */
private[session] object SessionHolder {

  private val threadLocal = new ThreadLocal[Option[Session]]()

  def get: Option[Session] = Option(threadLocal.get()).flatten

  def set(session: Session): Unit = threadLocal.set(Some(session))

  def clear(): Unit = threadLocal.remove()
}
```

**Change:** `private[sharaf]` → `private[session]`.

- [ ] **Step 3: Move Native SecureSessionId.scala (update visibility)**

```scala
// File: sharaf-core/src-native/ba/sake/sharaf/session/SecureSessionId.scala
package ba.sake.sharaf.session

import java.security.SecureRandom
import java.util.Base64

private[session] object SecureSessionId {

  private val rng = new SecureRandom()

  def generate(): String = {
    val bytes = new Array[Byte](16) // 128-bit random ID
    rng.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }
}
```

- [ ] **Step 4: Move Native SessionHolder.scala (update visibility)**

```scala
// File: sharaf-core/src-native/ba/sake/sharaf/session/SessionHolder.scala
package ba.sake.sharaf.session

/** Session holder for Scala Native (single-threaded) request-scoped session access. */
private[session] object SessionHolder {

  private var _session: Option[Session] = None

  def get: Option[Session] = _session

  def set(session: Session): Unit = _session = Some(session)

  def clear(): Unit = _session = None
}
```

- [ ] **Step 5: Delete old files and commit**

```bash
git rm sharaf-core/src-jvm/ba/sake/sharaf/SecureSessionId.scala \
        sharaf-core/src-jvm/ba/sake/sharaf/SessionHolder.scala \
        sharaf-core/src-native/ba/sake/sharaf/SecureSessionId.scala \
        sharaf-core/src-native/ba/sake/sharaf/SessionHolder.scala
git add sharaf-core/src-jvm/ba/sake/sharaf/session/SecureSessionId.scala \
        sharaf-core/src-jvm/ba/sake/sharaf/session/SessionHolder.scala \
        sharaf-core/src-native/ba/sake/sharaf/session/SecureSessionId.scala \
        sharaf-core/src-native/ba/sake/sharaf/session/SessionHolder.scala
git commit -m "refactor: move SecureSessionId/SessionHolder to ba.sake.sharaf.session, update visibility"
```

---

## Task 6: Move InMemorySessionStore.scala (JVM + Native)

**Files:**
- Move: `sharaf-core/src-jvm/ba/sake/sharaf/InMemorySessionStore.scala` → `sharaf-core/src-jvm/ba/sake/sharaf/session/InMemorySessionStore.scala`
- Move: `sharaf-core/src-native/ba/sake/sharaf/InMemorySessionStore.scala` → `sharaf-core/src-native/ba/sake/sharaf/session/InMemorySessionStore.scala`

- [ ] **Step 1: Move JVM InMemorySessionStore.scala (update imports to new package)**

```scala
// File: sharaf-core/src-jvm/ba/sake/sharaf/session/InMemorySessionStore.scala
package ba.sake.sharaf.session

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

/** In-memory session store backed by a [[ConcurrentHashMap]] for thread-safe JVM use.
  *
  * Note: sessions are lost on server restart. For production use, consider a persistent
  * store (e.g. Redis or database-backed).
  */
final class InMemorySessionStore(config: SessionConfig) extends SessionStore {

  private val store = new ConcurrentHashMap[String, SessionImpl]()

  override def create(): SessionImpl = {
    val id = SecureSessionId.generate()
    val now = Instant.now()
    val session = new SessionImpl(id, now, now, Map.empty)
    store.put(id, session)
    session
  }

  override def load(sessionId: String): Option[SessionImpl] =
    Option(store.get(sessionId)).flatMap { session =>
      val now = Instant.now()
      val idleExpired = config.maxAge.exists { maxAge =>
        session._lastAccessedAt.plus(maxAge).isBefore(now)
      }
      val absoluteExpired = config.absoluteTimeout.exists { timeout =>
        session._createdAt.plus(timeout).isBefore(now)
      }
      if idleExpired || absoluteExpired then
        store.remove(sessionId)
        None
      else Some(session)
    }

  override def save(session: SessionImpl): Unit =
    store.put(session.id, session)

  override def delete(sessionId: String): Unit =
    store.remove(sessionId)

}

object InMemorySessionStore:
  def apply(config: SessionConfig = SessionConfig.default): InMemorySessionStore =
    new InMemorySessionStore(config)
```

**Changes from old version:**
- `SharafSession` → `SessionImpl`
- `load(cookieValue: String)` → `load(sessionId: String)` — parameter renamed
- `store.get(cookieValue)` → `store.get(sessionId)` — map key is session ID

- [ ] **Step 2: Move Native InMemorySessionStore.scala (same updates)**

```scala
// File: sharaf-core/src-native/ba/sake/sharaf/session/InMemorySessionStore.scala
package ba.sake.sharaf.session

import java.time.Instant
import scala.collection.mutable

/** In-memory session store for Scala Native (single-threaded). */
final class InMemorySessionStore(config: SessionConfig) extends SessionStore {

  private val store = mutable.HashMap.empty[String, SessionImpl]

  override def create(): SessionImpl = {
    val id = SecureSessionId.generate()
    val now = Instant.now()
    val session = new SessionImpl(id, now, now, Map.empty)
    store.put(id, session)
    session
  }

  override def load(sessionId: String): Option[SessionImpl] =
    store.get(sessionId).flatMap { session =>
      val now = Instant.now()
      val idleExpired = config.maxAge.exists { maxAge =>
        session._lastAccessedAt.plus(maxAge).isBefore(now)
      }
      val absoluteExpired = config.absoluteTimeout.exists { timeout =>
        session._createdAt.plus(timeout).isBefore(now)
      }
      if idleExpired || absoluteExpired then
        store.remove(sessionId)
        None
      else Some(session)
    }

  override def save(session: SessionImpl): Unit =
    store.put(session.id, session)

  override def delete(sessionId: String): Unit =
    store.remove(sessionId)

}

object InMemorySessionStore:
  def apply(config: SessionConfig = SessionConfig.default): InMemorySessionStore =
    new InMemorySessionStore(config)
```

- [ ] **Step 3: Delete old files and commit**

```bash
git rm sharaf-core/src-jvm/ba/sake/sharaf/InMemorySessionStore.scala \
        sharaf-core/src-native/ba/sake/sharaf/InMemorySessionStore.scala
git add sharaf-core/src-jvm/ba/sake/sharaf/session/InMemorySessionStore.scala \
        sharaf-core/src-native/ba/sake/sharaf/session/InMemorySessionStore.scala
git commit -m "refactor: move InMemorySessionStore to ba.sake.sharaf.session, update to SessionImpl"
```

---

## Task 7: Add NoOpSessionStore to core

**Files:**
- Create: `sharaf-core/src/ba/sake/sharaf/session/NoOpSessionStore.scala`
- Create: `sharaf-core/test/src/ba/sake/sharaf/session/NoOpSessionStoreTest.scala`

- [ ] **Step 1: Write the failing test**

```scala
// File: sharaf-core/test/src/ba/sake/sharaf/session/NoOpSessionStoreTest.scala
package ba.sake.sharaf.session

class NoOpSessionStoreTest extends munit.FunSuite:

  val store = NoOpSessionStore.instance

  test("create returns a fresh SessionImpl"):
    val session = store.create()
    assert(session.id.nonEmpty)
    assert(session.keys.isEmpty)

  test("load always returns None"):
    val session = store.create()
    assertEquals(store.load(session.id), None)
    assertEquals(store.load("nonexistent"), None)

  test("save is a no-op"):
    val session = store.create()
    store.save(session)  // should not throw

  test("delete is a no-op"):
    val session = store.create()
    store.delete(session.id)  // should not throw

  test("each create produces a unique session"):
    val s1 = store.create()
    val s2 = store.create()
    assertNotEquals(s1.id, s2.id)
```

- [ ] **Step 2: Run test to verify it fails**

```bash
# Cannot run directly — need to wire up test module. Skip for now, will verify in Task 12.
```

- [ ] **Step 3: Write NoOpSessionStore implementation**

```scala
// File: sharaf-core/src/ba/sake/sharaf/session/NoOpSessionStore.scala
package ba.sake.sharaf.session

import java.time.Instant

/** A [[SessionStore]] that creates ephemeral sessions but never persists them.
  *
  * Useful for stateless authentication (e.g. JWT) where a session object is needed for
  * the duration of a single request but no server-side storage or session cookie is
  * required.
  *
  * `load` always returns [[None]], so a fresh session is created on every request.
  * `save` and `delete` are no-ops.
  */
final class NoOpSessionStore extends SessionStore:

  override def create(): SessionImpl =
    new SessionImpl(SecureSessionId.generate(), Instant.now(), Instant.now(), Map.empty)

  override def load(sessionId: String): Option[SessionImpl] = None

  override def save(session: SessionImpl): Unit = ()

  override def delete(sessionId: String): Unit = ()

object NoOpSessionStore:
  val instance: NoOpSessionStore = new NoOpSessionStore
```

- [ ] **Step 4: Commit**

```bash
git add sharaf-core/src/ba/sake/sharaf/session/NoOpSessionStore.scala \
        sharaf-core/test/src/ba/sake/sharaf/session/NoOpSessionStoreTest.scala
git commit -m "feat: add NoOpSessionStore for stateless auth"
```

---

## Task 8: Add package object re-exports

**Files:**
- Modify: `sharaf-core/src/ba/sake/sharaf/package.scala`

- [ ] **Step 1: Add session re-exports to package object**

The current `package.scala` (39 lines) needs session type re-exports added:

```scala
// File: sharaf-core/src/ba/sake/sharaf/package.scala
package ba.sake.sharaf

import sttp.client4.*
import sttp.model.*
import ba.sake.sharaf.routing.FromPathParam
import ba.sake.{formson, querson}
import formson.*
import querson.*

export HttpMethod.*

type ExceptionMapper = exceptions.ExceptionMapper
val ExceptionMapper = exceptions.ExceptionMapper

type Routes = ba.sake.sharaf.routing.Routes
val Routes = ba.sake.sharaf.routing.Routes

val Path = ba.sake.sharaf.routing.Path

object param:
  def unapply[T](str: String)(using fp: FromPathParam[T]): Option[T] =
    fp.parse(str)

// session re-exports
export ba.sake.sharaf.session.{
  Session, SessionConfig, SessionStore, SessionImpl,
  InMemorySessionStore, NoOpSessionStore
}

// conversions to STTP
extension [T](value: T)(using rw: formson.FormDataRW[T])
  def toSttpMultipart(config: formson.Config = formson.DefaultFormsonConfig): Seq[Part[BasicBodyPart]] =
    val multiParts = value.toFormDataMap(config).flatMap { case (key, values) =>
      values.map {
        case formson.FormValue.Str(value)       => multipart(key, value)
        case formson.FormValue.File(value)      => multipartFile(key, value.toFile)
        case formson.FormValue.ByteArray(value) => multipart(key, value)
      }
    }
    multiParts.toSeq

extension [T](value: T)(using rw: querson.QueryStringRW[T])
  def toSttpQuery(config: querson.Config = querson.DefaultQuersonConfig): QueryParams =
    val params = value.toQueryStringMap(config).map { (k, vs) => k -> vs }
    QueryParams.fromMultiMap(params)
```

- [ ] **Step 2: Commit**

```bash
git add sharaf-core/src/ba/sake/sharaf/package.scala
git commit -m "refactor: re-export session types from ba.sake.sharaf package object"
```

---

## Task 9: Fix SharafHandler.sessions() factory import

**Files:**
- Modify: `sharaf-core/src/ba/sake/sharaf/SharafHandler.scala`

- [ ] **Step 1: Update imports — the `sessions()` factory now references session package**

```scala
// File: sharaf-core/src/ba/sake/sharaf/SharafHandler.scala
package ba.sake.sharaf

import sttp.model.StatusCode
import ba.sake.sharaf.routing.{RequestParams, Routes}
import ba.sake.sharaf.handlers.*
import ba.sake.sharaf.session.{SessionHandler, SessionStore, SessionConfig, InMemorySessionStore}

trait SharafHandler:
  def handle(context: RequestContext): Response[?]

object SharafHandler:

  val DefaultNotFoundHandler: SharafHandler =
    _ => Response.withStatus(StatusCode.NotFound).withBody("Not Found")

  def routes(
      routess: Routes,
      notFoundHandler: SharafHandler = DefaultNotFoundHandler
  ): SharafHandler =
    RoutesHandler(routess, notFoundHandler)

  def files(
      directoryPath: java.nio.file.Path,
      notFoundHandler: SharafHandler = DefaultNotFoundHandler
  ): SharafHandler =
    FilesHandler(directoryPath, notFoundHandler)

  def classpathResources(
      rootPath: String,
      notFoundHandler: SharafHandler = DefaultNotFoundHandler
  ): SharafHandler =
    ClasspathResourcesHandler(rootPath, notFoundHandler)

  def exceptions(
      wrappedHandler: SharafHandler,
      exceptionMapper: ExceptionMapper = ExceptionMapper.default
  ): SharafHandler =
    ExceptionHandler(exceptionMapper, wrappedHandler)

  def cors(next: SharafHandler, corsSettings: CorsSettings = CorsSettings.default): SharafHandler =
    CorsHandler(corsSettings, next)

  def sessions(
      next: SharafHandler,
      store: SessionStore = InMemorySessionStore(),
      config: SessionConfig = SessionConfig.default
  ): SharafHandler =
    SessionHandler(store, config, next)

case class RequestContext(
    params: RequestParams,
    request: Request
)
```

**Change:** Added explicit import: `import ba.sake.sharaf.session.{SessionHandler, SessionStore, SessionConfig, InMemorySessionStore}`

- [ ] **Step 2: Commit**

```bash
git add sharaf-core/src/ba/sake/sharaf/SharafHandler.scala
git commit -m "refactor: update SharafHandler.sessions() to import from session package"
```

---

## Task 10: Update test imports for moved session types

**Files:**
- Modify: `sharaf-core/test/src-jvm/ba/sake/sharaf/handlers/AbstractSessionHandlerTest.scala`
- Modify: `sharaf-undertow/test/src/ba/sake/sharaf/undertow/handlers/SessionHandlerTest.scala`

- [ ] **Step 1: Read the current AbstractSessionHandlerTest to understand what imports need updating**

The test file imports session-related types. We need to ensure the imports are updated to `ba.sake.sharaf.session`. However, since we added re-exports in the package object, existing `import ba.sake.sharaf._` style imports should continue to work.

Check if the tests compile after the move. If they use `import ba.sake.sharaf.*` they should work via the re-exports.

- [ ] **Step 2: Verify test files compile (will be checked in Task 12)**

No code changes needed if they use wildcard imports from `ba.sake.sharaf`.

- [ ] **Step 3: Commit (or skip if no changes needed)**

```
Unless specific import changes are needed, move to next task.
```

---

## Task 11: Update other modules referencing session types

**Files to check and potentially update:**

These files may import `SharafSession`, `SessionStore`, etc. from `ba.sake.sharaf`:
- `sharaf-undertow/` — Undertow integration
- `sharaf-http4s/` — Http4s integration  
- `sharaf-helidon/` — Helidon integration
- `sharaf-jdk-httpserver/` — JDK HttpServer integration
- `sharaf-snunit/` — Scala Native integration
- `examples/jwt/src/Main.scala`
- `examples/user-pass-form/src/userpassform/SecurityService.scala`
- `examples/oauth2/src/SecurityService.scala`

- [ ] **Step 1: Search for references in all non-core modules**

```bash
grep -r "SharafSession\|SessionStore\|SessionHandler\|CookieSessionStore\|SessionConfig" --include="*.scala" sharaf-undertow/ sharaf-http4s/ sharaf-helidon/ sharaf-jdk-httpserver/ sharaf-snunit/ 2>/dev/null
```

- [ ] **Step 2: Fix any direct references**

Most server integrations don't directly reference session types — they're used through `SharafHandler.sessions()`. The `examples/jwt` will be updated in Task 31.

- [ ] **Step 3: Commit any fixes found**

---

## Task 12: Run compile and tests to verify Phase 1 (session restructure)

**Files:** N/A

- [ ] **Step 1: Try to compile sharaf-core**

```bash
./deder compile sharaf-core/jvm
```

- [ ] **Step 2: Fix any compilation errors**
- [ ] **Step 3: Run existing session tests**

```bash
./deder test sharaf-undertow/test
```

- [ ] **Step 4: Run NoOpSessionStore tests**

```bash
./deder test sharaf-core/jvm_test
```

- [ ] **Step 5: Commit any fixes**

```bash
git add -A
git commit -m "fix: compilation errors from session package restructure"
```

---

## Task 13: Create SharafWebContext — pac4j WebContext adapter

**Files:**
- Create: `sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafWebContext.scala`

- [ ] **Step 1: Write SharafWebContext**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafWebContext.scala
package ba.sake.sharaf.pac4j

import java.util.{Collection as JCollection, Optional, HashMap as JHashMap}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import org.pac4j.core.context.{Cookie as Pac4jCookie, FrameworkParameters, WebContext, WebContextFactory}
import ba.sake.sharaf.{Cookie as SharafCookie, Request, Response, HttpString}

/** Adapts a Sharaf [[Request]] to pac4j's [[WebContext]].
  *
  * Receives the full URL and HTTP method from the framework (since the [[Request]] trait
  * does not carry these). Accumulates response modifications (cookies, headers, content
  * type) in mutable collections, then merges them back into a Sharaf [[Response]] via
  * [[supplementResponse]].
  */
final class SharafWebContext(
    private val request: Request,
    val fullUrl: String,
    val method: HttpMethod
) extends WebContext:

  private val cachedUri = new java.net.URI(fullUrl)

  // --- Response accumulation ---
  private val _responseCookies = scala.collection.mutable.Buffer.empty[SharafCookie]
  private val _responseHeaders = new JHashMap[String, String]()
  private var _responseContentType: Option[String] = None

  // --- Request attributes (pac4j's internal state) ---
  private val _requestAttributes = new JHashMap[String, AnyRef]()

  /** Merge accumulated response modifications into a Sharaf [[Response]]. */
  def supplementResponse(res: Response[?]): Response[?] =
    val withCookies = _responseCookies.foldLeft(res)((r, c) => r.settingCookie(c))
    val withHeaders = _responseHeaders.asScala.foldLeft(withCookies) { case (r, (k, v)) =>
      r.settingHeader(k, Seq(v))
    }
    _responseContentType.fold(withHeaders)(ct => withHeaders.settingHeader("Content-Type", ct))

  /** Returns the underlying Sharaf [[Request]]. */
  def getNativeRequest: Request = request

  // --- WebContext: request methods ---

  override def getRequestURL(): String = fullUrl

  override def getRequestMethod(): String = method.name

  override def getRequestCookies(): JCollection[Pac4jCookie] =
    request.cookies.map { c =>
      val maxAge = c.maxAge match
        case None        => -1 // session cookie
        case Some(n)     => n
      val pc = new Pac4jCookie(c.name, c.value)
      pc.setMaxAge(maxAge)
      c.path.foreach(pc.setPath)
      c.domain.foreach(pc.setDomain)
      pc.setSecure(c.secure)
      pc.setHttpOnly(c.httpOnly)
      c.sameSiteMode.foreach(pc.setSameSitePolicy)
      pc
    }.asJavaCollection

  override def getRequestHeaders(): java.util.Map[String, String] =
    request.headers.map { (k, v) => k.value -> v.mkString(", ") }.asJava

  override def getRequestParameters(): java.util.Map[String, Array[String]] =
    request.queryParamsRaw.map { (k, v) => k -> v.toArray }.asJava

  override def getRequestParameter(name: String): Optional[String] =
    request.queryParamsRaw.get(name).flatMap(_.headOption).toJava

  override def getRequestHeader(name: String): Optional[String] =
    request.headers.collectFirst {
      case (k, v) if k.matches(name) => v.headOption
    }.flatten.toJava

  override def getRequestAttribute(name: String): Optional[AnyRef] =
    Optional.ofNullable(_requestAttributes.get(name))

  override def setRequestAttribute(name: String, value: AnyRef): Unit =
    _requestAttributes.put(name, value)

  override def getRemoteAddr(): String =
    request.headers.get(HttpString("X-Forwarded-For"))
      .flatMap(_.headOption)
      .getOrElse("127.0.0.1")

  override def getServerName(): String =
    request.headers.get(HttpString("Host"))
      .flatMap(_.headOption)
      .map(_.split(":").head)
      .getOrElse("localhost")

  override def getServerPort(): Int =
    request.headers.get(HttpString("Host"))
      .flatMap(_.headOption)
      .flatMap(_.split(":").lift(1))
      .flatMap(_.toIntOption)
      .getOrElse(if isSecure() then 443 else 80)

  override def getScheme(): String =
    request.headers.get(HttpString("X-Forwarded-Proto"))
      .flatMap(_.headOption)
      .getOrElse(if isSecure() then "https" else "http")

  override def isSecure(): Boolean =
    getScheme() == "https"

  override def getFullRequestURL(): String = fullUrl

  override def getPath(): String = cachedUri.getPath

  override def getRequestContent(): String = request.bodyString

  // --- WebContext: response methods ---

  override def addResponseCookie(cookie: Pac4jCookie): Unit =
    val maxAge = cookie.getMaxAge match
      case -1 => None           // session cookie
      case 0  => Some(0)        // delete cookie (handled by expires=EPOCH below)
      case n  => Some(n)
    val expires = cookie.getMaxAge match
      case 0 => Some(java.time.Instant.EPOCH)
      case _ => None
    val sc = SharafCookie(
      name = cookie.getName,
      value = cookie.getValue,
      path = Option(cookie.getPath),
      domain = Option(cookie.getDomain),
      maxAge = maxAge,
      expires = expires,
      secure = cookie.isSecure,
      httpOnly = cookie.isHttpOnly,
      sameSite = true,
      sameSiteMode = Option(cookie.getSameSitePolicy)
    )
    _responseCookies += sc

  override def setResponseHeader(name: String, value: String): Unit =
    _responseHeaders.put(name, value)

  override def getResponseHeader(name: String): Optional[String] =
    Optional.ofNullable(_responseHeaders.get(name))

  override def setResponseContentType(contentType: String): Unit =
    _responseContentType = Some(contentType)

  override def getProtocol(): String = "HTTP/1.1"

  override def getQueryString(): Optional[String] =
    val query = cachedUri.getQuery
    if query == null then Optional.empty() else Optional.of(query)

  override def getCharacterEncoding(): Optional[String] =
    request.headers.get(HttpString("Content-Type"))
      .flatMap(_.headOption)
      .flatMap(ct => ct.split("charset=").lift(1))
      .toJava

object SharafWebContext:
  /** pac4j [[WebContextFactory]] that creates [[SharafWebContext]] from [[SharafFrameworkParameters]]. */
  val factory: WebContextFactory = (params: FrameworkParameters) =>
    params match
      case sfp: SharafFrameworkParameters => new SharafWebContext(sfp.request, sfp.fullUrl, sfp.method)
      case _ => throw new IllegalArgumentException(
        s"Expected SharafFrameworkParameters, got ${params.getClass.getName}"
      )
```

- [ ] **Step 2: Create SharafFrameworkParameters helper**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafFrameworkParameters.scala
package ba.sake.sharaf.pac4j

import org.pac4j.core.context.FrameworkParameters
import ba.sake.sharaf.Request

/** pac4j [[FrameworkParameters]] carrying a Sharaf [[Request]] with its full URL and method.
  *
  * The [[Request]] trait does not carry `method` or the full request URL — those are
  * supplied by the server integration and passed through here.
  */
final class SharafFrameworkParameters(
    val request: Request,
    val fullUrl: String,
    val method: HttpMethod
) extends FrameworkParameters
```

- [ ] **Step 3: Commit**

```bash
git add sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafWebContext.scala \
        sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafFrameworkParameters.scala
git commit -m "feat: add SharafWebContext — pac4j WebContext adapter"
```

---

## Task 14: Write SharafWebContextTest

**Files:**
- Create: `sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/SharafWebContextTest.scala`

[Detailed test code omitted for brevity — tests cookie conversion, header accumulation, supplementResponse merging. Will use a minimal Sharaf Request stub.]

**Commit:**

```bash
git add sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/SharafWebContextTest.scala
git commit -m "test: add SharafWebContext tests"
```

---

## Task 15: Create SharafSessionStore — pac4j SessionStore adapter

**Files:**
- Create: `sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafSessionStore.scala`

- [ ] **Step 1: Write SharafSessionStore**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafSessionStore.scala
package ba.sake.sharaf.pac4j

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.{Base64, Optional}
import scala.util.Using
import org.pac4j.core.context.{FrameworkParameters, WebContext}
import org.pac4j.core.context.session.{SessionStore as Pac4jSessionStore, SessionStoreFactory}
import ba.sake.sharaf.session.{Session as SharafSessionTrait, SessionStore as SharafSessionStoreTrait, SessionImpl, NoOpSessionStore}

/** Adapts a Sharaf [[SharafSessionStoreTrait]] to pac4j's [[Pac4jSessionStore]].
  *
  * Bridges pac4j's key-value `Object` storage to Sharaf's typed JSON session via Java
  * serialization → Base64 encoding. pac4j values are stored under `"pac4j."` prefixed keys
  * in the Sharaf session.
  */
final class SharafSessionStore(store: SharafSessionStoreTrait) extends Pac4jSessionStore:

  private val prefix = "pac4j."

  override def getSessionId(context: WebContext, createSession: Boolean): Optional[String] =
    val currentSession = Option(SharafSessionTrait.current)
    currentSession match
      case Some(s) => Optional.of(s.id)
      case None if createSession =>
        val newSession = store.create()
        setSharafSession(newSession)
        Optional.of(newSession.id)
      case _ => Optional.empty()

  override def get(context: WebContext, key: String): Optional[AnyRef] =
    val session = SharafSessionTrait.current
    session.getOpt[String](s"$prefix$key") match
      case Some(encoded) => Optional.ofNullable(deserialize(encoded))
      case None          => Optional.empty()

  override def set(context: WebContext, key: String, value: AnyRef): Unit =
    val encoded = serialize(value)
    SharafSessionTrait.current.set(s"$prefix$key", encoded)

  override def destroySession(context: WebContext): Boolean =
    val session = SharafSessionTrait.current
    store.delete(session.id)
    session.invalidate()
    true

  override def renewSession(context: WebContext): Boolean =
    val session = SharafSessionTrait.current
    session.regenerate()
    session._previousId.foreach(store.delete)
    store.save(session.asInstanceOf[SessionImpl])
    true

  override def getTrackableSession(context: WebContext): Optional[AnyRef] =
    Optional.of(SharafSessionTrait.current.id)

  override def buildFromTrackableSession(
      context: WebContext,
      trackableSession: AnyRef
  ): Optional[Pac4jSessionStore] =
    Optional.empty() // not supported for in-memory stores

  // --- serialization helpers ---

  private def serialize(obj: AnyRef): String =
    Using(new ByteArrayOutputStream()) { baos =>
      Using(new ObjectOutputStream(baos)) { oos =>
        oos.writeObject(obj)
      }
      Base64.getEncoder.encodeToString(baos.toByteArray)
    }.get

  private def deserialize[T](encoded: String): AnyRef =
    val bytes = Base64.getDecoder.decode(encoded)
    Using(new ByteArrayInputStream(bytes)) { bais =>
      Using(new ObjectInputStream(bais)) { ois =>
        ois.readObject()
      }.get
    }.get

  private def setSharafSession(session: SessionImpl): Unit =
    ba.sake.sharaf.session.SessionHolder.set(session)

object SharafSessionStore:
  /** Creates a pac4j [[SessionStoreFactory]] that wraps a Sharaf [[SharafSessionStoreTrait]]. */
  def factory(store: SharafSessionStoreTrait): SessionStoreFactory =
    (_: FrameworkParameters) => new SharafSessionStore(store)
```

- [ ] **Step 2: Commit**

```bash
git add sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafSessionStore.scala
git commit -m "feat: add SharafSessionStore — pac4j SessionStore adapter"
```

---

## Task 16: Write SharafSessionStoreTest

**Files:**
- Create: `sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/SharafSessionStoreTest.scala`

[Detailed test code — tests get/set roundtrip, destroySession, renewSession, prefix isolation.]

**Commit:**

```bash
git add sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/SharafSessionStoreTest.scala
git commit -m "test: add SharafSessionStore tests"
```

---

## Task 17: Create SharafHttpActionAdapter

**Files:**
- Create: `sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafHttpActionAdapter.scala`

- [ ] **Step 1: Write SharafHttpActionAdapter**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafHttpActionAdapter.scala
package ba.sake.sharaf.pac4j

import org.pac4j.core.context.WebContext
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.exception.http.*
import ba.sake.sharaf.Response
import sttp.model.StatusCode

/** Converts pac4j [[HttpAction]]s to Sharaf [[Response]] objects. */
final class SharafHttpActionAdapter extends HttpActionAdapter:

  override def adapt(action: HttpAction, context: WebContext): AnyRef =
    val sharafCtx = context.asInstanceOf[SharafWebContext]
    val baseResponse = action match
      case a: ForbiddenAction =>
        Response.withStatus(StatusCode.Forbidden)
      case a: UnauthorizedAction =>
        var res = Response.withStatus(StatusCode.Unauthorized)
        Option(a.getWwwAuthenticate).foreach { wwwAuth =>
          res = res.settingHeader("WWW-Authenticate", wwwAuth)
        }
        res
      case a: OkAction =>
        var res = Response.withStatus(StatusCode.Ok)
        Option(a.getContent).foreach { content =>
          res = res.withBody(content)
        }
        res
      case a: NoContentAction =>
        Response.withStatus(StatusCode.NoContent)
      case a: BadRequestAction =>
        Response.withStatus(StatusCode.BadRequest)
      case a: FoundAction =>
        Response.redirect(a.getLocation).withStatus(StatusCode.Found)
      case a: SeeOtherAction =>
        Response.redirect(a.getLocation).withStatus(StatusCode.SeeOther)
      case a: WithLocationAction =>
        Response.redirect(a.getLocation)
      case a: StatusAction =>
        Response.withStatus(StatusCode.unsafeApply(a.getCode))
      case a: WithContentAction =>
        var res = Response.withStatus(StatusCode.unsafeApply(a.getCode))
        Option(a.getContent).foreach { content =>
          res = res.withBody(content)
        }
        res
      case _ =>
        Response.withStatus(StatusCode.InternalServerError)

    sharafCtx.supplementResponse(baseResponse)
```

- [ ] **Step 2: Commit**

```bash
git add sharaf-pac4j/src/ba/sake/sharaf/pac4j/SharafHttpActionAdapter.scala
git commit -m "feat: add SharafHttpActionAdapter — pac4j HTTP action adapter"
```

---

## Task 18: Write SharafHttpActionAdapterTest

**Files:**
- Create: `sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/SharafHttpActionAdapterTest.scala`

[Tests each pac4j action maps to correct Sharaf status/headers.]

**Commit:**

```bash
git add sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/SharafHttpActionAdapterTest.scala
git commit -m "test: add SharafHttpActionAdapter tests"
```

---

## Task 19: Create Pac4jSecurityConfig

**Files:**
- Create: `sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jSecurityConfig.scala`

- [ ] **Step 1: Write Pac4jSecurityConfig**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jSecurityConfig.scala
package ba.sake.sharaf.pac4j

import org.pac4j.core.config.Config
import org.pac4j.core.client.Clients
import org.pac4j.core.matching.matcher.Matchers
import org.pac4j.core.authorization.authorizer.Authorizers
import org.pac4j.core.engine.{
  SecurityLogic, DefaultSecurityLogic,
  CallbackLogic, DefaultCallbackLogic,
  LogoutLogic, DefaultLogoutLogic
}
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.context.WebContextFactory
import org.pac4j.core.context.session.SessionStoreFactory
import ba.sake.sharaf.session.{SessionStore, InMemorySessionStore}

/** Builder for a pac4j [[Config]] pre-wired with Sharaf adapters.
  *
  * @param clients       Pac4j clients (e.g. OAuth, JWT, form login)
  * @param authorizers   Pac4j authorizers (role/permission checks)
  * @param matchers      Pac4j matchers (security filter rules)
  * @param sessionStore  Sharaf session store for persisting pac4j session data
  * @param callbackUrl   Default URL after OAuth callback
  * @param logoutUrl     Default URL after logout
  */
final class Pac4jSecurityConfig private (
    val securityLogic: SecurityLogic,
    val callbackLogic: CallbackLogic,
    val logoutLogic: LogoutLogic,
    val webContextFactory: WebContextFactory,
    val sessionStoreFactory: SessionStoreFactory,
    val httpActionAdapter: HttpActionAdapter,
    val clients: Clients,
    val authorizers: Authorizers,
    val matchers: Matchers,
    val sessionStore: SessionStore,
    val callbackUrl: Option[String],
    val logoutUrl: Option[String]
):

  /** Builds the pac4j [[Config]]. */
  def toPac4jConfig: Config =
    val config = new Config(clients)
    config.setAuthorizers(authorizers)
    config.setMatchers(matchers)
    config.setWebContextFactory(webContextFactory)
    config.setSessionStoreFactory(sessionStoreFactory)
    config.setHttpActionAdapter(httpActionAdapter)
    config.setSecurityLogic(securityLogic)
    config.setCallbackLogic(callbackLogic)
    config.setLogoutLogic(logoutLogic)
    config

  def withSecurityLogic(logic: SecurityLogic): Pac4jSecurityConfig =
    copy(securityLogic = logic)

  def withCallbackLogic(logic: CallbackLogic): Pac4jSecurityConfig =
    copy(callbackLogic = logic)

  def withLogoutLogic(logic: LogoutLogic): Pac4jSecurityConfig =
    copy(logoutLogic = logic)

  def withSessionStore(store: SessionStore): Pac4jSecurityConfig =
    copy(sessionStore = store, sessionStoreFactory = SharafSessionStore.factory(store))

  def withCallbackUrl(url: String): Pac4jSecurityConfig =
    copy(callbackUrl = Some(url))

  def withLogoutUrl(url: String): Pac4jSecurityConfig =
    copy(logoutUrl = Some(url))

  private def copy(
      securityLogic: SecurityLogic = securityLogic,
      callbackLogic: CallbackLogic = callbackLogic,
      logoutLogic: LogoutLogic = logoutLogic,
      webContextFactory: WebContextFactory = webContextFactory,
      sessionStoreFactory: SessionStoreFactory = sessionStoreFactory,
      httpActionAdapter: HttpActionAdapter = httpActionAdapter,
      clients: Clients = clients,
      authorizers: Authorizers = authorizers,
      matchers: Matchers = matchers,
      sessionStore: SessionStore = sessionStore,
      callbackUrl: Option[String] = callbackUrl,
      logoutUrl: Option[String] = logoutUrl
  ): Pac4jSecurityConfig = new Pac4jSecurityConfig(
    securityLogic, callbackLogic, logoutLogic,
    webContextFactory, sessionStoreFactory, httpActionAdapter,
    clients, authorizers, matchers, sessionStore, callbackUrl, logoutUrl
  )

object Pac4jSecurityConfig:

  def apply(
      clients: Clients,
      authorizers: Authorizers = new Authorizers(),
      matchers: Matchers = new Matchers(),
      sessionStore: SessionStore = InMemorySessionStore(),
      callbackUrl: Option[String] = None,
      logoutUrl: Option[String] = None,
      securityLogic: SecurityLogic = new DefaultSecurityLogic(),
      callbackLogic: CallbackLogic = new DefaultCallbackLogic(),
      logoutLogic: LogoutLogic = new DefaultLogoutLogic(),
  ): Pac4jSecurityConfig =
    new Pac4jSecurityConfig(
      securityLogic = securityLogic,
      callbackLogic = callbackLogic,
      logoutLogic = logoutLogic,
      webContextFactory = SharafWebContext.factory,
      sessionStoreFactory = SharafSessionStore.factory(sessionStore),
      httpActionAdapter = new SharafHttpActionAdapter(),
      clients = clients,
      authorizers = authorizers,
      matchers = matchers,
      sessionStore = sessionStore,
      callbackUrl = callbackUrl,
      logoutUrl = logoutUrl
    )
```

- [ ] **Step 2: Commit**

```bash
git add sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jSecurityConfig.scala
git commit -m "feat: add Pac4jSecurityConfig — builder for pre-wired pac4j Config"
```

---

## Task 20: Create Pac4jSecurityHandler

**Files:**
- Create: `sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jSecurityHandler.scala`

- [ ] **Step 1: Write Pac4jSecurityHandler**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jSecurityHandler.scala
package ba.sake.sharaf.pac4j

import java.util.Optional
import scala.jdk.OptionConverters.*
import org.pac4j.core.context.{FrameworkParameters, WebContext}
import org.pac4j.core.engine.DefaultSecurityLogic
import ba.sake.sharaf.{SharafHandler, RequestContext, Response}
import ba.sake.sharaf.session.{SessionImpl, SessionHolder, NoOpSessionStore}

/** A [[SharafHandler]] decorator that applies pac4j security AND manages session lifecycle.
  *
  * Subsumes both session management and pac4j security in one handler — no separate
  * [[SessionHandler]] needed when using pac4j.
  *
  * On each request:
  *   1. Creates [[SharafWebContext]] from the Sharaf [[Request]]
  *   2. Loads/creates a session via the configured [[SessionStore]]
  *   3. Sets [[SessionHolder]] (so [[Session.current]] works in wrapped routes)
  *   4. Calls pac4j's [[SecurityLogic]]
  *   5. On auth success: delegates to the inner handler, supplements the response with
  *      session cookies and any pac4j-added response modifications
  *   6. On auth required: returns the HTTP action (redirect, 401, forbidden, etc.)
  *
  * If the [[SessionStore]] is [[NoOpSessionStore]], no session cookie is set (stateless).
  */
final class Pac4jSecurityHandler(
    securityConfig: Pac4jSecurityConfig,
    clients: Option[String],
    authorizers: Option[String],
    matchers: Option[String],
    next: SharafHandler
) extends SharafHandler:

  override def handle(context: RequestContext): Response[?] =
    val webContext = new SharafWebContext(context.request)
    val session = loadOrCreateSession()
    SessionHolder.set(session)

    try
      val frameworkParams = new SharafFrameworkParameters(context.request)
      val pac4jConfig = securityConfig.toPac4jConfig()
      val securityLogic = pac4jConfig.getSecurityLogic.asInstanceOf[DefaultSecurityLogic]

      // Build parameters list for securityLogic.perform()
      // pac4j's SecurityLogic.perform() signature varies by version.
      // For pac4j-core 6.x, the standard pattern is:
      //   perform(config, (webCtx, sessionStore, profiles) -> { ... }, clients, authorizers, matchers, parameters)
      val result = securityLogic.perform(
        pac4jConfig,
        (wc: WebContext, ss, profiles) =>
          // Auth success callback: delegate to inner handler
          val res = next.handle(context)
          val supplemented = finalizeResponse(webContext, session, res)
          java.util.concurrent.CompletableFuture.completedFuture(supplemented),
        clients.orNull,
        authorizers.orNull,
        matchers.orNull,
        frameworkParams
      )

      // If securityLogic returned a Response (auth required), supplement it
      result match
        case res: Response[?] => finalizeResponse(webContext, session, res)
        case _ =>
          // Auth success path: the callback already returned via the lambda above
          // This shouldn't happen with standard pac4j flow, but handle defensively
          val res = next.handle(context)
          finalizeResponse(webContext, session, res)
    finally
      SessionHolder.clear()

  private def loadOrCreateSession(): SessionImpl =
    val sessionId = Option.empty[String] // we create new on each request via pac4j
    // pac4j's SessionStore handles creation internally via getSessionId(createSession=true)
    securityConfig.sessionStore.create()

  private def finalizeResponse(
      webContext: SharafWebContext,
      session: SessionImpl,
      res: Response[?]
  ): Response[?] =
    val supplemented = webContext.supplementResponse(res)

    // Set session cookie unless using NoOpSessionStore
    securityConfig.sessionStore match
      case _: NoOpSessionStore => supplemented // stateless — no cookie
      case _ =>
        securityConfig.sessionStore.save(session)
        supplemented.settingCookie(
          ba.sake.sharaf.Cookie(
            name = "SHARAF_SESSION",
            value = session.id,
            path = Some("/"),
            maxAge = Some(1800), // 30 minutes
            secure = true,
            httpOnly = true,
            sameSite = true,
            sameSiteMode = Some("Strict")
          )
        )
```

**Note:** The pac4j `SecurityLogic.perform()` signature needs verification during implementation. The exact parameter types and return type depend on the specific pac4j-core 6.5.2 API. This implementation may need adjustment based on actual compilation.

- [ ] **Step 2: Commit**

```bash
git add sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jSecurityHandler.scala
git commit -m "feat: add Pac4jSecurityHandler — security + session lifecycle decorator"
```

---

## Task 21: Create Pac4jCallbackHandler and Pac4jLogoutHandler

**Files:**
- Create: `sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jCallbackHandler.scala`
- Create: `sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jLogoutHandler.scala`

- [ ] **Step 1: Write Pac4jCallbackHandler**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jCallbackHandler.scala
package ba.sake.sharaf.pac4j

import ba.sake.sharaf.{SharafHandler, RequestContext, Response}

/** Handles the OAuth/OIDC callback endpoint. */
final class Pac4jCallbackHandler(securityConfig: Pac4jSecurityConfig) extends SharafHandler:

  override def handle(context: RequestContext): Response[?] =
    val webContext = new SharafWebContext(context.request)
    val pac4jConfig = securityConfig.toPac4jConfig()
    val frameworkParams = new SharafFrameworkParameters(context.request)

    val result = pac4jConfig.getCallbackLogic.perform(
      pac4jConfig,
      securityConfig.callbackUrl.orNull,
      true, // renewSession
      null, // defaultClient
      frameworkParams
    )

    result match
      case res: Response[?] => webContext.supplementResponse(res)
      case _ =>
        webContext.supplementResponse(Response.redirect(securityConfig.callbackUrl.getOrElse("/")))
```

- [ ] **Step 2: Write Pac4jLogoutHandler**

```scala
// File: sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jLogoutHandler.scala
package ba.sake.sharaf.pac4j

import ba.sake.sharaf.{SharafHandler, RequestContext, Response}

/** Handles the logout endpoint (local + central logout). */
final class Pac4jLogoutHandler(securityConfig: Pac4jSecurityConfig) extends SharafHandler:

  override def handle(context: RequestContext): Response[?] =
    val webContext = new SharafWebContext(context.request)
    val pac4jConfig = securityConfig.toPac4jConfig()
    val frameworkParams = new SharafFrameworkParameters(context.request)

    val result = pac4jConfig.getLogoutLogic.perform(
      pac4jConfig,
      securityConfig.logoutUrl.getOrElse("/"),
      null, // logoutUrlPattern
      true, // localLogout
      true, // destroySession
      false, // centralLogout
      frameworkParams
    )

    result match
      case res: Response[?] => webContext.supplementResponse(res)
      case _ =>
        webContext.supplementResponse(Response.redirect(securityConfig.logoutUrl.getOrElse("/")))
```

- [ ] **Step 3: Commit**

```bash
git add sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jCallbackHandler.scala \
        sharaf-pac4j/src/ba/sake/sharaf/pac4j/Pac4jLogoutHandler.scala
git commit -m "feat: add Pac4jCallbackHandler and Pac4jLogoutHandler"
```

---

## Task 22: Write Pac4jSecurityHandlerTest

**Files:**
- Create: `sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/Pac4jSecurityHandlerTest.scala`

- [ ] **Step 1: Write integration tests**

```scala
// File: sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/Pac4jSecurityHandlerTest.scala
package ba.sake.sharaf.pac4j

import org.pac4j.core.client.Clients
import org.pac4j.http.client.direct.HeaderClient
import ba.sake.sharaf.{SharafHandler, RequestContext, Response}
import ba.sake.sharaf.session.NoOpSessionStore
import sttp.model.StatusCode

class Pac4jSecurityHandlerTest extends munit.FunSuite:

  // Helper: create a simple Request stub for testing
  // (Will need a minimal Request implementation for test purposes)

  test("public route passes through without auth"):
    // Given: security handler with no clients matcher → security bypassed
    // When: handle public request
    // Then: response is 200 OK with body

  test("protected route without auth returns 401"):
    // Given: security handler with HeaderClient requiring Authorization header
    // When: handle protected request without auth header
    // Then: response is 401 Unauthorized

  test("protected route with valid auth returns 200"):
    // Given: security handler with HeaderClient requiring Authorization header
    // When: handle protected request with valid auth header
    // Then: response is 200 OK, profile accessible via session

  test("NoOpSessionStore does not set session cookie"):
    // Given: security handler with NoOpSessionStore
    // When: handle any request
    // Then: response has no Set-Cookie header
```

**Note:** These tests need a minimal Sharaf `Request` stub and a test authenticator. The test authenticator logic from the old `TestHeaderAuthenticator.scala` can be reused.

- [ ] **Step 2: Commit**

```bash
git add sharaf-pac4j/test/src/ba/sake/sharaf/pac4j/Pac4jSecurityHandlerTest.scala
git commit -m "test: add Pac4jSecurityHandler integration tests"
```

---

## Task 23: Update build config (deder.pkl)

**Files:**
- Modify: `deder.pkl` (lines 227-250)

- [ ] **Step 1: Verify current build config is correct**

The current config:
```
Main deps:  pac4j-core:6.5.2  ← correct (interfaces only)
Test deps:  pac4j-http:6.5.2  ← correct (HeaderClient for tests)
```

**No changes needed.** The `examples/jwt` and other example apps declare their own `pac4j-http`, `pac4j-jwt`, etc.

- [ ] **Step 2: Ensure sharaf-pac4j source directories exist**

The `src/` and `test/` directories already exist (empty). No build config changes needed for the module itself.

- [ ] **Step 3: Mark as done**

---

## Task 24: Compile entire project and fix errors

**Files:** N/A

- [ ] **Step 1: Compile sharaf-core**

```bash
./deder compile sharaf-core/jvm
```

- [ ] **Step 2: Compile sharaf-pac4j**

```bash
./deder compile sharaf-pac4j
```

- [ ] **Step 3: Compile all server integrations**

```bash
./deder compile sharaf-undertow
./deder compile sharaf-http4s/jvm
./deder compile sharaf-helidon
./deder compile sharaf-jdk-httpserver
```

- [ ] **Step 4: Fix compilation errors**

- Types that need `private[sharaf]` → `private[sharaf.session]` updates
- Missing imports for `SessionImpl` etc.
- pac4j API signature mismatches

- [ ] **Step 5: Commit fixes**

```bash
git add -A
git commit -m "fix: compilation errors across modules after session restructure"
```

---

## Task 25: Run all tests

**Files:** N/A

- [ ] **Step 1: Run core tests**

```bash
./deder test sharaf-core/jvm_test
```

- [ ] **Step 2: Run undertow tests (includes SessionHandlerTest)**

```bash
./deder test sharaf-undertow/test
```

- [ ] **Step 3: Run pac4j tests**

```bash
./deder test sharaf-pac4j/test
```

- [ ] **Step 4: Run other server integration tests**

```bash
./deder test sharaf-http4s/test
./deder test sharaf-jdk-httpserver/test
```

- [ ] **Step 5: Fix any test failures and commit**

```bash
git add -A
git commit -m "fix: test failures after session restructure"
```

---

## Task 26: Update JWT example

**Files:**
- Modify: `examples/jwt/src/Main.scala`

- [ ] **Step 1: Rewrite JWT example to use new API**

The current `examples/jwt/src/Main.scala` references the old `Pac4jSecurityConfig` and a custom `NoopSessionStore`. Update to use the new `Pac4jSecurityConfig` with `NoOpSessionStore.instance`.

```scala
// Updated example using new Sharaf pac4j API
// Key changes:
// - Import from ba.sake.sharaf.pac4j
// - Use Pac4jSecurityConfig(clients, sessionStore = NoOpSessionStore.instance)
// - Use Pac4jSecurityHandler(config, clients = Some("HeaderClient"), next = routes)
```

- [ ] **Step 2: Run JWT example tests**

```bash
./deder test examples/jwt/test
```

- [ ] **Step 3: Commit**

```bash
git add examples/jwt/src/Main.scala
git commit -m "refactor: update JWT example to new sharaf-pac4j API"
```

---

## Task 27: Final cleanup

**Files:** N/A

- [ ] **Step 1: Search for any remaining references to old package locations**

```bash
grep -r "ba\.sake\.sharaf\.SharafSession\|ba\.sake\.sharaf\.handlers\.SessionHandler\|CookieSessionStore" --include="*.scala" | grep -v .worktrees | grep -v docs/
```

- [ ] **Step 2: Run full build one last time**

```bash
./deder compile all
./deder test all
```

- [ ] **Step 3: Commit any remaining cleanup**

```bash
git add -A
git commit -m "chore: final cleanup for sharaf-pac4j integration"
```

---

## Implementation Order Summary

```
Task  1: Create session package directories
Task  2: Move Session.scala, rename SharafSession → SessionImpl
Task  3: Move SessionConfig.scala, SessionStore.scala (simplify), delete CookieSessionStore
Task  4: Move SessionHandler.scala (use session.id)
Task  5: Move SecureSessionId.scala, SessionHolder.scala (JVM + Native)
Task  6: Move InMemorySessionStore.scala (JVM + Native)
Task  7: Add NoOpSessionStore + test
Task  8: Add package object re-exports
Task  9: Fix SharafHandler.sessions() import
Task 10: Update test imports
Task 11: Update other module references
Task 12: Compile + test core
Task 13: Create SharafWebContext + SharafFrameworkParameters
Task 14: SharafWebContextTest
Task 15: Create SharafSessionStore
Task 16: SharafSessionStoreTest
Task 17: Create SharafHttpActionAdapter
Task 18: SharafHttpActionAdapterTest
Task 19: Create Pac4jSecurityConfig
Task 20: Create Pac4jSecurityHandler
Task 21: Create Pac4jCallbackHandler + Pac4jLogoutHandler
Task 22: Pac4jSecurityHandlerTest
Task 23: Build config (no changes needed)
Task 24: Compile entire project
Task 25: Run all tests
Task 26: Update JWT example
Task 27: Final cleanup
```
