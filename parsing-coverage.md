# Sharaf request parsing coverage map

Source review, 2026-11-11. Sharaf HEAD 0b442007daf815121f065c9b000cfcc325168fb9; deder.pkl selects Undertow 2.4.3.Final and Potateh 0.2.1-SNAPSHOT. The separately inspected Potateh checkout is c55f45be95dcaff765ac84c918fc520a22a1393b; its correspondence to the resolved snapshot was not verified. This is an inventory, not a test run or a conformance finding.

## Layers

- HTTP server: Undertow and Potateh parse request lines, headers and framing before the adapters run. Potateh's RequestHeadParser and body streams implement that boundary, with configurable request, header, chunk, trailer and body limits. Undertow's exact parser behavior and configuration remain to be evaluated against its selected dependency.
- Paths: Undertow's SharafUndertowHandler obtains the request URI and accounts for mounted prefix segments. PotatehSharafRequest converts the target to a URI and takes its raw path. Both then use RequestDecoding.path: split before decoding each segment once, retain plus, reject invalid percent escapes and malformed UTF-8.
- Headers: adapters expose server-provided values in a case-insensitive HttpString-keyed map. Repeated values per name are retained; global ordering across names is not represented.
- Cookies: both use CookieHeaders on the exposed Cookie headers, preserving duplicate names and opaque values. Undertow passes its configured MAX_COOKIES; Potateh uses the shared default of 200. Session lookup behavior is separately covered.
- Queries and URL-encoded forms: both use UrlEncoded; splitting precedes one UTF-8 decode, repeated values are grouped, malformed escapes are errors and malformed encoded UTF-8 is replaced. Form key insertion order is represented; query returns a map, not a wire-order sequence. URL-encoded body reads have a 1 MiB limit. Formson and Querson provide typed conversion after raw parsing.
- Multipart: both use shared Multipart, with default limits of 32 MiB total, 16 MiB per file, 64 KiB per field, 128 parts, 16 KiB and 32 headers per part. Uploads are temporary files. Undertow closes uploads after response writing; Potateh registers response-completion cleanup.
- Other bodies: bodyString reads as UTF-8; JSON decoding uses Tupson via Request. This inventory does not establish bounds for non-form bodies.

## Existing tests

- Shared real-server suites in sharaf-core/test/src/ba/sake/sharaf/handlers are extended by both adapter test modules. AbstractSharafHandlerTest covers repeated headers, encoded path segments, query/form repetition, encoding and content types. AbstractErrorHandlerTest covers mapping failures and malformed URL-encoded form escapes. AbstractUrlEncodedLimitTest covers fixed-length and chunked limits. AbstractCookieTest and AbstractDuplicateCookieTest cover repeated headers/names, opaque values, limits and session lookup. AbstractMultipartTest covers typed fields and files, failures, cleanup and response lifetime.
- Shared unit suites UrlEncodedTest, CookieHeadersTest and MultipartTest cover additional edge cases including malformed escapes and UTF-8, delimiters, binary payloads, boundaries, all multipart limits and cleanup on parser failure. Formson and Querson have typed conversion suites; PathTest primarily checks matching rather than HTTP decoding.
- Undertow-specific tests include mounted encoded paths and configured cookie limits. PotatehSharafServerTest includes invalid UTF-8 paths, malformed URIs, conflicting framing, recovery and upload-completion scenarios. These tests are not a comprehensive HTTP protocol corpus.
- Potateh has its own transport tests in its separate repository; these are not evidence that the specific resolved snapshot or Sharaf integration was tested in this review.

## Platforms and gaps

- Undertow is JVM-only; Potateh is configured for JVM and Scala Native 0.5.12. Shared parser and integration test sources are configured across relevant targets, but test execution for this inventory was not performed.
- Existing tests give useful targeted coverage, not coverage against an external corpus. Catalog malformed request targets and raw-header/framing cases at both server boundaries; compare content-type parameters, charset handling, duplicate/ambiguous fields, multipart variants, limits and cleanup through real servers. Client libraries can normalize inputs, so cases requiring exact bytes need raw HTTP.
- Snapshot resolution and the exact Potateh server revision need recording before making version-specific claims. Undertow parser settings and effective limits also need recording. Other adapters, proxies and platforms have not been mapped here.
- External suite selection, execution and security review are subsequent work and have not been performed.

## Candidate evaluation: Cookie Crumbles

Reviewed 2026-11-11. The MIT-licensed [server-side toolchain](https://github.com/SecPriv/cookiecrumbles/tree/main/reflectors) generates three-character cookie keys, sends each as `key=foo` to three HTTP reflectors, and records differing JSON responses. It includes a CSV of 18,472 observed discrepancies, not a set of authoritative expected results. Its PHP, ReactPHP and Werkzeug versions are deliberately pinned to versions predating the authors' fixes.

The fuzzer requires Python and `requests`; the supplied reflectors use Docker Compose. A Sharaf reflector exposing parsed cookie names and values as JSON could be added for each adapter, with the fuzzer's hard-coded service list updated. Errors are currently recorded as empty strings, so transport failures must be distinguished from parser differences. The cases exercise unusual names with a fixed value, rather than covering all cookie syntax or repeated headers.

Recommendation: useful for discovering suspicious differences on Undertow and Potateh, but assess each result against Sharaf's contract and relevant specifications rather than treating the old reflectors as an oracle.

### Potateh and Werkzeug comparison

On 2026-11-11, a temporary harness sent 16 selected Cookie headers to real local Sharaf Potateh JVM and Werkzeug 3.1.8 development servers. Both returned HTTP 200 and the same name/value pairs in 13 cases, including duplicate names, empty values, opaque percent escapes and the three examples listed in Cookie Crumbles. The comparison sorted pairs, so it did not verify their order. It did not run the Cookie Crumbles fuzzer or test Scala Native.

All three differences involved quotes. For `quoted="abc=="`, Sharaf preserved the quotes and Werkzeug removed them. For `a="x y"` the same difference appeared; for `a="x;y"`, Sharaf split at the semicolon while Werkzeug retained it inside the value. Sharaf's opaque-value behavior is already covered by its cookie tests. RFC 6265 permits quoted values but its cookie-octet grammar excludes spaces and semicolons, even inside quotes; these observations alone do not establish a conformance or security defect.

The full Potateh JVM test module passed 113 tests, including the temporary comparison harness, which checked that the comparison ran but did not assert parity. The harness and output remain under ignored build output and `/tmp`; no permanent test or behavior change was made.

### Generated cookie names

An adapted harness generated all 59,319 three-character keys from the [Cookie Crumbles fuzzer](https://github.com/SecPriv/cookiecrumbles/blob/main/reflectors/fuzz.py), each with the fixed value `foo`, and sent them over HTTP to Sharaf Potateh JVM and Werkzeug 3.1.8. Both returned the same sorted name/value pairs for every generated header; no request errors were reported. The upstream script itself was not run: the harness used Python's urllib and current Werkzeug rather than its requests client and original reflector versions. HTTP clients and servers can normalize headers, and this comparison did not check ordering, duplicate headers, other values, Native, or conformance against a specification.

The full Potateh JVM test module passed 113 tests including the temporary harness. Its first run completed the comparison but failed MUnit's 30-second timeout; a rerun with a three-minute harness timeout passed with the same counts. No new regression test was added because the generated cases revealed no differences.

## Candidate evaluation: Rack multifail

Reviewed 2026-11-11 at [8e92f778470c72fcc48185de2335233471d2b8c2](https://github.com/rack/multifail/tree/8e92f778470c72fcc48185de2335233471d2b8c2), whose latest commit is dated 2019-11-15. License: Apache-2.0. No local execution was performed.

- The repository contains a [Ruby generator](https://github.com/rack/multifail/blob/8e92f778470c72fcc48185de2335233471d2b8c2/maketests.rb) for HTML forms and upload files, and a [Go request dumper](https://github.com/rack/multifail/blob/8e92f778470c72fcc48185de2335233471d2b8c2/dumper.go). Generated forms, upload files and recorded request dumps are not present in the checked-in tree; the index alone is not a runnable corpus. There are no supplied parser assertions or expected results.
- The generator combines page and form charsets with empty and populated text/file inputs, multiple file inputs, non-ASCII and reserved field names, and varied filenames. It does not set the declared field values in generated HTML, so filling fields and selecting files requires a driver. Its encoding check calls `encode` without using the result; generated cases and failures need verification. The script uses `URI.escape`, which may require adapting for modern Ruby. These are source observations, not an execution result.
- The intended workflow requires Ruby, Go and browser submissions. The dumper records HTTP requests but does not validate a parser. To test Sharaf, generate selected forms and automate browser input, capture the multipart Content-Type and body, then send those bytes to real Undertow and Potateh endpoints exposing fields and file bytes. A separate manifest must record inputs and expected values, accounting for browser encoding, empty file inputs and Sharaf's API. Raw replay should preserve the captured body and boundary; Go's `DumpRequest` is not necessarily a byte-exact capture of the original HTTP framing.
- Its README predates [RFC 7578](https://www.rfc-editor.org/rfc/rfc7578) and is not a conformance oracle. Review charset, repeated parts and filenames against the RFC and the intended Sharaf contract; browser behavior alone does not establish the correct interpretation. It supplies browser-generated cases, not malformed multipart or security coverage.
- Recommendation: do not adopt as the primary automated conformance suite. Its case ideas can inform a small browser interoperability experiment, but generating an oracle and browser automation is substantial work; evaluate alternatives with existing wire vectors and expectations first. Docker may isolate tooling but does not supply the missing corpus or assertions.

### Browser interoperability experiment

On 2026-11-11, Playwright drove Chromium 156.0.8078.4 to submit four HTML multipart forms to real local Sharaf servers on the JVM: Undertow 2.4.3.Final and Potateh 0.2.1-SNAPSHOT (resolved snapshot revision not verified). Forms were hand-written from multifail's scenarios; the original generator was not run. Both adapters passed:

- Empty text and an unselected file input: empty text and an empty file were exposed.
- Unicode and reserved field names: `téxt`, `t%xt`, and `t+xt` and their submitted values were preserved.
- A file named `a filé%+.bin`: the five bytes `00 01 7f 80 ff` were preserved.
- Two files under the same field: both byte sequences were preserved in order.

The handler compared raw form keys, values and uploaded file bytes; Sharaf's `FormValue.File` exposes a temporary path, so the submitted filename and media type were not checked. This does not cover legacy charsets, malformed bodies, other browsers or Native. The temporary harness and Potateh log are in `/tmp/sharaf-browser-experiment/`; they are not part of the project. Full JVM test module runs, including the temporary experiment, passed: Undertow 108 tests and Potateh 113 tests. No CI run was performed.
