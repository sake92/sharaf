# Request parsing and security plan

## Goal

Evaluate Sharaf's request parsing against external test corpora, specifications, and other server implementations. Start with Undertow and Potateh, then extend applicable tests to the other supported adapters and platforms. Follow parsing work with a focused security review. Do not add benchmarks.

## 1. Map coverage and evaluate external suites

- Map where each layer parses or transforms requests: the HTTP server, Sharaf adapter, and shared Sharaf code. Record existing test coverage and platform limitations.
- Evaluate each candidate's maintenance, license, test cases, expected results, dependencies, and integration effort before choosing it. Record the revision used.
- Start with these candidates:

| Candidate | Scope | Evaluation notes |
| --- | --- | --- |
| [Rack multifail](https://github.com/rack/multifail) | Multipart form edge-case corpus | Assess the generated cases and how to send them to a real Sharaf server. Its README contains outdated specification language; check expectations against current specifications. |
| [Cookie Crumbles](https://github.com/SecPriv/cookiecrumbles) | Server-side cookie parser testing and security research | Assess whether its server-side toolchain can exercise Sharaf and whether its dependencies are practical to run. |
| [Web Platform Tests: URL](https://github.com/web-platform-tests/wpt/tree/master/url) | URL and URL-encoded parsing vectors | Select cases applicable to Sharaf's server-side behavior; browser expectations are not automatically server requirements. |
| [HTTP Garden](https://github.com/narfindustries/http-garden) | HTTP/1.1 server and proxy differential testing and fuzzing | Assess integration of a Sharaf/Potateh target and comparison with existing servers. It targets HTTP message parsing, not Sharaf's form or cookie APIs. |
| [OWASP CRS multipart regression tests](https://github.com/coreruleset/coreruleset/blob/main/tests/regression/tests/REQUEST-922-MULTIPART-ATTACK/922110.yaml) | Multipart inspection and security bypass cases | Evaluate in the security phase, including WAF/backend differences. WAF rule results are not directly Sharaf conformance verdicts. |

Use the [OWASP Web Security Testing Guide](https://owasp.org/www-project-web-security-testing-guide/) as security guidance, including parameter pollution; it is not itself a complete executable parsing corpus.

## 2. Run parsing cases against real servers

- Build a small test application that reports the values Sharaf exposes for paths, queries, headers, cookies, URL-encoded forms, and multipart fields and files. Exercise Undertow and Potateh first, then applicable other adapters.
- Use external cases wherever applicable, including malformed and ambiguous inputs. Send raw HTTP where clients would otherwise normalize the input.
- Check decoded values, repetitions and ordering where exposed, errors, limits, binary uploads, and upload cleanup. Include encoded delimiters, decoding depth, malformed escapes and UTF-8, duplicate cookies and parameters, content types, charsets, and multipart boundaries.
- Compare applicable cases against selected established server implementations. Investigate differences against the relevant specifications and intended Sharaf contract; a disagreement alone is not a defect.
- Fix confirmed defects and retain focused regression tests. Record deliberate differences, unsupported cases, and untested adapters or platforms.

## 3. Check HTTP protocol boundaries

- Evaluate HTTP Garden and other suitable external tools for request framing, malformed headers, connection reuse, and HTTP/1.1 parsing discrepancies. Include proxy-to-backend combinations when investigating request smuggling.
- Identify whether each finding belongs to the underlying server, the adapter, or Sharaf. Report configuration and tested versions; do not claim complete HTTP conformance from parser tests alone.

## 4. Security review

- Use the parsing findings and external security cases to review path traversal and normalization, duplicate-input ambiguity, header injection, resource limits, slow requests, and proxy trust.
- Review cookies, sessions, CSRF, CORS, and authentication integration. Decide how `SharafWebContext` should handle form parsing failures currently treated as an empty body.
- Where a proxy or WAF is involved, compare its interpretation with the backend's. Add regression tests for confirmed issues and document deployment-dependent boundaries.

## Evidence and completion

Record suite revisions, tested server and platform versions, configurations, commands, results, deviations, and limitations. Separate local results from CI and distinguish tested behavior from unverified claims. The work is complete when the selected external cases have been evaluated, applicable failures resolved or explicitly documented, and the security review findings addressed or recorded for follow-up.
