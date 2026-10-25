# Local native deployment measurements

Medians across fresh processes. See `methodology.txt`, `metadata.json`, `results.json`, and build logs.

| Variant | Artifact MiB | First HTTP ms | Idle RSS MiB | After requests RSS MiB | Build seconds | Build max RSS MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| jdk-jvm | 13.86 | 266.3 | 66.8 | 91.4 | 6.9 | 298.9 |
| jdk-graalvm | 17.69 | 21.8 | 20.0 | 30.5 | 57.8 | 1430.7 |
| http4s-jvm | 36.70 | 1413.8 | 183.2 | 201.8 | 12.3 | 347.6 |
| http4s-native-fast | 22.12 | 30.1 | 30.9 | 43.6 | 133.4 | 3490.8 |
| http4s-native-size | 21.81 | 31.8 | 30.5 | 43.2 | 123.7 | 3061.9 |
| undertow-jvm | 17.71 | 454.5 | 91.2 | 102.8 | 7.5 | 304.0 |

## Native executable stripping

| Variant | As packaged MiB | After `strip --strip-unneeded` MiB |
| --- | ---: | ---: |
| jdk-graalvm | 17.69 | 17.69 |
| http4s-native-fast | 22.12 | 18.39 |
| http4s-native-size | 21.81 | 18.00 |

GraalVM already emitted a stripped executable; Scala Native retained symbols. Stripping was measured separately on
copies after the timed runs. Each stripped copy passed real HTTP checks for JSON GET/POST, Unicode, invalid input (400),
and missing routes (404). Runtime timing/RSS observations above refer to the original packaged executables. Commands,
byte counts, and the strip version are recorded in `stripped-sizes.json`.

## Environment and scope

- Linux x86_64, Intel Core i7-1165G7, Scala CLI 1.16.0, Scala 3.9.0, published Sharaf 0.20.0.
- JVM applications: Temurin 21.0.10. Native Image: GraalVM CE 25.0.2, default O2, `-march=compatibility`, Serial GC.
  These JVM/GraalVM figures use different JDK major versions; they are deployment examples, not a compiler-only experiment.
- http4s/Ember 1.0.0-M48, Scala Native 0.5.12, multithreading enabled, Immix GC, no LTO.
- Local Clang 14.0.0 emitted a deprecation warning; Scala Native recommends Clang 16 or newer. See retained build logs.
- The installed full JDK occupies 362,127,166 bytes (345.35 MiB); it is excluded from JAR sizes. This is not the size of a
  minimal jlink runtime. Native executables also depend on OS shared libraries, listed in `build-details/`.
- Medians use five fresh processes per variant. First HTTP is a successful `GET /plaintext`, observed by a preinitialized
  external HTTP client polling every 5 ms. Idle RSS is sampled 1 second later. Post-request RSS is sampled 1 second after
  300 sequential mixed plain-text/JSON GET/JSON POST requests, following six HTTP smoke checks.
- Build times are one observation each with retained compiler/dependency caches. GNU time maximum RSS is not aggregate
  concurrent process-tree memory. Startup uses a warm host/filesystem; no throughput, peak concurrent-load RSS, container
  size, TLS, realistic application, or Lambda cold-start claims are made.
- The examples explicitly set body headers for compatibility with published Sharaf 0.20.0's http4s adapter. The local
  adapter fix is tested separately on JVM and Scala Native; these measurements use published artifacts.
- The source hashes match the files in this change. `metadata.json` records the base revision and dirty working tree
  before this change was committed; complete per-variant package/run commands and samples are in `results.json`.
- snunit is excluded: its example compiled, but linking failed because this workstation lacks `libunit`. A future snunit
  comparison must include both the external Unit daemon and application workers.

See [the measurement guide](../../README.md) for reproduction commands.
