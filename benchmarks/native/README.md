# Measure native deployment examples

Recorded baseline: [Linux x86_64, 2026-10-25](results/2026-10-25-linux-x86_64/README.md).

Run these commands from the repository root on Linux with Scala CLI, Java 21, GNU `time`, `clang` (16 or newer
recommended), and the Scala Native [system dependencies](https://scala-native.org/en/stable/user/setup.html).
Scala CLI downloads the pinned JVM/GraalVM and library dependencies. Keep port 8181 available.

```sh
scala scripts/measure-native.scala --server=false -- --output out/native-measurements
```

Choose a new output directory for each run. To check only one deployment or shorten a smoke run:

```sh
scala scripts/measure-native.scala --server=false -- \
  --output out/graalvm-check --only jdk-graalvm --runs 1 --requests 3
```

Repeat `--only` to select several variants. Available variants are `jdk-jvm`, `jdk-graalvm`, `http4s-jvm`,
`http4s-native-fast`, `http4s-native-size`, and `undertow-jvm`.

Open the generated `results.md` for the summary and `results.json` for raw repetitions. Keep `metadata.json`,
`methodology.txt`, source snapshots/checksums, dynamic-library lists, and per-variant logs with the results.
If a build or HTTP check fails, inspect its log; the runner exits nonzero and does not record a successful measurement.

To measure a stripped deployment copy of a native executable while preserving the packaged artifact:

```sh
strip --strip-unneeded -o /tmp/sharaf-native-stripped out/native-measurements/http4s-native-fast/app
wc -c out/native-measurements/http4s-native-fast/app /tmp/sharaf-native-stripped
```

Start the stripped copy and run the HTTP requests from the example before deploying it. GraalVM output may already be
stripped; retain both byte counts rather than assuming the packaging tools strip symbols in the same way.

Use the JDK pair to compare JVM/GraalVM and the http4s pair to compare JVM/Scala Native. All variants use the same
plain-text and JSON routes. Compare the two Scala Native modes to examine size/performance tradeoffs.

The examples use published Sharaf 0.20.0 and explicitly set response content types to accommodate its http4s adapter's
missing inferred body headers. The adapter fix in this repository is tested separately; measurements do not silently
substitute locally changed library artifacts for the published release.

The native examples use dynamically linked system libraries. JVM artifact sizes are fat JARs and exclude the JVM;
the runner records the installed full JDK size separately. Startup means a fresh process serving its first successful
HTTP response on a warm host, not a machine-cold or AWS Lambda cold start. RSS after sequential requests is not peak
memory under concurrent load. Build timings retain dependency/compiler caches and include any required downloads.

The scheduled/manual `Native examples and measurements` workflow runs the same command and uploads results/logs.
Its hosted-runner numbers should be assessed separately from workstation measurements.
