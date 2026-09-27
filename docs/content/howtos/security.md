---
layout: howto.html
title: Secure a production application
description: Configure Sharaf and pac4j authentication for a production deployment
---

# {{ page.title }}

This guide applies to Sharaf {{ site.data.project.artifact.version }} and its `sharaf-pac4j` integration. It assumes a
TLS-terminating reverse proxy and an application that is already reachable through its final public URL. Use the
[secure production defaults](/reference/secure-production-defaults.html) as the release checklist.

## Start deny-by-default

Put the application behind one `Pac4jSecurityHandler`. Register a `PathMatcher` that excludes only the endpoints that
must be public: typically the landing page, login page, callback, static assets, health endpoint, and logout endpoint.
Keep the callback and logout paths in both the matcher and `Pac4jSecurityConfig`; the handler processes those paths
before it delegates to application routes.

```scala
import scala.jdk.CollectionConverters.*
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.matching.matcher.{DefaultMatchers, PathMatcher}
import ba.sake.sharaf.*
import ba.sake.sharaf.pac4j.*

val callbackPath = "/callback"
val clients = Clients(callbackPath, formClient) // use the client for your login mechanism
val pac4jConfig = Config(clients)

val publicRoutes = PathMatcher()
publicRoutes.excludePaths("/", "/login", callbackPath, "/logout", "/health")
publicRoutes.excludeBranch("/assets")
pac4jConfig.addMatcher("publicRoutes", publicRoutes)

val security = Pac4jSecurityConfig(
  pac4jConfig,
  clients = clients.getClients.asScala.map(_.getName()).mkString(","),
  matchers = s"${DefaultMatchers.SECURITYHEADERS},publicRoutes"
).withCallbackPath(callbackPath)
 .withLogoutPath("/logout")
 .withDefaultLogoutUrl("/")

val handler = Pac4jSecurityHandler(security, SharafHandler.routes(routes))
```

Do not exclude a broad application branch merely to make a route work. Add an explicit public endpoint instead.
`DefaultMatchers.SECURITYHEADERS` enables pac4j's security-header matcher; add a Content Security Policy and the
HTTPS-specific headers at the proxy as described in the defaults page.

## Choose one authentication flow

### Form login

Use an indirect `FormClient` with a real user repository and a deliberately slow password hash such as bcrypt. The
[`user-pass-form` example]({{ site.data.project.gh.sourcesUrl }}/examples/user-pass-form) shows Sharaf's wiring.
Serve the login form and callback over HTTPS, rate-limit login attempts at the edge, and return the same failure message
for unknown users and bad passwords. Do not put credentials in query parameters or logs.

The callback executed by `Pac4jSecurityHandler` renews the session after successful authentication. Keep that behavior:
it changes the session identifier and protects against session fixation. Configure logout with `.withLogoutPath`; the
handler performs local logout and destroys the pac4j session.

### OAuth2 and OpenID Connect

Use pac4j's indirect OAuth/OIDC client, a callback URL that exactly matches the provider registration, and a fixed
public base URL. Load the client ID and secret from deployment secrets, not source code or a checked-in config file.
Validate issuer, audience, signature, expiry, and redirect URI through the provider-specific pac4j client settings.
Do not accept arbitrary `returnUrl` values after login; permit only local, allow-listed paths.

The [`oauth2` example]({{ site.data.project.gh.sourcesUrl }}/examples/oauth2) demonstrates the redirect flow. Treat it
as a development example: use the `sharaf-pac4j` handler for new applications rather than the older Undertow-specific
pac4j handler used there.

### JWT for an API

Use a direct `HeaderClient` and `NoOpSessionStore` for a stateless API. Supply a high-entropy signing key through a
secret manager, require a `Bearer` authorization scheme, set short expirations, and validate issuer/audience when your
token design has them. Rotate keys deliberately; never print generated tokens or signing keys at startup.

The [`jwt` example]({{ site.data.project.gh.sourcesUrl }}/examples/jwt) shows the handler shape. Its inline secret and
printed test token are intentionally not production configuration.

## Authorize separately from authentication

Authentication proves who made a request. Authorization decides whether that identity may perform the action. Register
pac4j authorizers by name and pass their comma-separated names through `Pac4jSecurityConfig.authorizers` for the routes
they protect.

```scala
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer

pac4jConfig.addAuthorizer("admin", RequireAnyRoleAuthorizer("admin"))

val adminSecurity = Pac4jSecurityConfig(
  pac4jConfig,
  clients = "HeaderClient",
  authorizers = "admin"
)
```

Ensure the client maps trusted identity-provider claims to pac4j roles before relying on a role authorizer. For resource
ownership rules such as “may edit this invoice”, enforce the check next to the route/domain operation after obtaining
`SecurityService.currentUser`; a role alone is not sufficient.

## Use a production session store

`InMemorySessionStore` is appropriate for local development and a single disposable process only. Sessions disappear on
restart and are not shared by replicas. For browser login, provide a `SessionStore` backed by Redis or a database and
pass it to `Pac4jSecurityConfig.withSessionStore`. `sharaf-jdbc-session` provides the JDBC option; it uses squery and
accepts any standard JDBC `DataSource` on the JVM or a Native-compatible JDBC driver such as SQLite on Scala Native.
Add the database driver and, on the JVM, a connection pool separately.

```scala
import ba.sake.sharaf.session.SessionConfig
import ba.sake.sharaf.session.jdbc.JdbcSessionStore

// Copy the database-specific template from sharaf-jdbc-session/resources into an
// application-owned, versioned Flyway/Liquibase migration and apply it first.
// The library never creates or migrates tables at runtime.

val sessions = JdbcSessionStore(dataSource, SessionConfig.default)

val security = Pac4jSecurityConfig(pac4jConfig, clients = "FormClient")
  .withSessionStore(sessions)
  .withCallbackPath("/callback")
  .withLogoutPath("/logout")
```

Copy one of `schema-h2.sql`, `schema-mysql.sql`, `schema-postgresql.sql`, or `schema-sqlite.sql` from
`sharaf-jdbc-session/resources/ba/sake/sharaf/session/jdbc/` into your application's migration directory and assign its
own version. These are templates, not automatically-discovered Flyway migrations, so this library cannot interfere with
an existing migration history. The store serializes each session value as JSON and atomically replaces the old ID when a
session is regenerated. It enforces idle and absolute expiry on load; call `sessions.deleteExpired()` periodically to
remove abandoned expired rows. Database and serialization failures are propagated, so monitor failed reads/writes.
Encrypt access to the backing service, restrict it to the application network, and test it with two application
instances: log in through one, use the cookie through the other, regenerate on login, then verify that logout and expiry
invalidate both IDs.

The handler emits a `SHARAF_SESSION` cookie with `Secure`, `HttpOnly`, `SameSite=Strict`, path `/`, and a 30-minute
maximum age. `Secure` means browser login requires HTTPS. If cross-site login is a requirement, design the cookie and
CSRF protections together rather than weakening `SameSite` by default.

## Protect browser forms

For every state-changing browser endpoint, require a CSRF token tied to the authenticated session and verify it on the
server before changing state. Render it as a hidden form input (or send it in a custom header for HTMX/AJAX) and use a
constant-time comparison. Check `Origin` or `Referer` as defense in depth. Do not rely on `SameSite` alone, and do not
apply browser CSRF rules to a bearer-token API whose credentials are not sent automatically by the browser.

Set a restrictive Content Security Policy for HTML responses and avoid inline scripts where practical. Pac4j's
`SECURITYHEADERS` matcher supplies baseline headers, but it does not replace a CSP, HSTS, `Referrer-Policy`, or
`Permissions-Policy` configured for your application.

## Deploy behind a trusted proxy

Terminate TLS at a proxy you operate and permit traffic to the Sharaf process only from that proxy. Strip client-supplied
`Forwarded`, `X-Forwarded-For`, `X-Forwarded-Host`, and `X-Forwarded-Proto` headers, then set trusted replacements at
the proxy. Do not use forwarded headers received directly from the internet for redirect URLs, callback URLs, audit
identity, or access-control decisions.

Configure the external HTTPS URL explicitly in your OAuth/OIDC provider and pac4j client. Test login, callback, and
logout through that URL—not only against `localhost`—before release.

## Keep secrets and requests bounded

Read signing keys, OAuth client secrets, database credentials, and encryption keys from the deployment environment or a
secret manager. Validate that required values are present and sufficiently strong during startup, redact them from
configuration dumps and errors, and rotate them with a documented procedure.

Set request and upload body-size limits at the reverse proxy and, where supported by the selected server adapter, at the
server. Apply timeouts and rate limits at the proxy. Return a controlled `413 Payload Too Large` for requests exceeding
the agreed limit; do not allow an oversized body to reach parsing or application code.

## Test the real security boundary

Use a stateful HTTP client in integration tests and run the server through the same handler composition as production.
At minimum, cover:

- unauthenticated access is rejected or redirected;
- valid and invalid form/OIDC/JWT credentials have the expected outcome;
- a user without the required role receives `403`;
- login regenerates the session and logout invalidates it;
- state-changing browser requests without a CSRF token fail;
- the session cookie and security headers are present on both success and denial responses;
- an oversized request is rejected; and
- the public HTTPS URL, proxy redirects, and OAuth callback work end-to-end.

The `sharaf-pac4j` test suite exercises form, JWT, roles, logout, session invalidation, and baseline security headers.
Keep application-specific authorization and CSRF tests alongside the application: they are part of its public security
contract.
