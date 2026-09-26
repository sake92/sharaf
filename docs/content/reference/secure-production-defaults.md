---
title: Secure production defaults
description: Production security baseline for Sharaf applications
---

# {{ page.title }}

This reference applies to Sharaf {{ site.data.project.artifact.version }} applications using `sharaf-pac4j`.

| Concern | Production baseline |
| --- | --- |
| Transport | HTTPS only; redirect HTTP at the trusted proxy; enable HSTS after confirming every public subdomain supports HTTPS. |
| Public surface | Protect all routes by default. Exclude only named public endpoints and static assets. |
| Browser session | `Secure`, `HttpOnly`, `SameSite=Strict`, narrow path/domain, short idle expiry, absolute expiry, regenerated after login, destroyed at logout. |
| Session storage | Use Redis or JDBC-backed `SessionStore` for replicas or restart-safe sessions. Do not use `InMemorySessionStore` in production. |
| API tokens | Use a `HeaderClient` plus `NoOpSessionStore`; validate signature, expiry, issuer and audience where applicable; rotate signing keys. |
| Credentials | Store bcrypt/Argon2 password hashes, rate-limit login, use generic login errors, never log passwords, tokens, cookies, or authorization headers. |
| OAuth/OIDC | Exact registered HTTPS callback URL; validate provider metadata and tokens; store client secrets outside source control; allow-list post-login redirects. |
| Authorization | Enforce roles with pac4j authorizers and enforce resource ownership in application code. Test denial paths. |
| CSRF | Verify a session-bound token on every unsafe browser request; use `Origin`/`Referer` as defense in depth. |
| Response headers | Enable `DefaultMatchers.SECURITYHEADERS`; add application-specific CSP, `Strict-Transport-Security`, `Referrer-Policy`, and `Permissions-Policy` at the proxy. |
| Proxy trust | Only trust forwarded headers added by an owned proxy. Strip all client-supplied forwarded headers before proxying. |
| Request limits | Bound request/upload size, timeout, and rate at the proxy and server adapter. Return `413` for oversized bodies. |
| Secrets | Validate required secrets at startup; use a secret manager or deployment environment; redact values and rotate them on a schedule. |
| Verification | Test authentication, authorization, CSRF, session regeneration/logout, headers, size limits, and external callback URLs through the deployed proxy. |

Sharaf's pac4j handler currently writes `SHARAF_SESSION` cookies as `Secure`, `HttpOnly`, `SameSite=Strict`, path `/`,
with a 30-minute maximum age. These are sensible browser defaults, but they do not make an HTTP deployment safe and do
not replace CSRF validation or a persistent store.

For setup and deployment details, use the [security guide](/howtos/security.html).
