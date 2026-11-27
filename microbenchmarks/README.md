# Microbenchmarks

Measures URL query decoding and Querson parsing on Scala Native. The cases cover plain, escaped, repeated and empty queries, plus flat, nested and indexed Querson inputs. Results are elapsed nanoseconds per operation; each case runs three warmup batches and five measured batches of 50,000 operations. A checksum consumes the results.

From the repository root, build an optimized executable and run it:

```sh
deder shutdown
SHARAF_SCALA_NATIVE_VERSION=0.5.13-20261107-69432d9-SNAPSHOT deder exec -t fullNativeLink -m micro-benchmarks
deder shutdown
./.deder/out/micro-benchmarks/fullNativeLink/micro-benchmarks
```

The command uses the Scala Native snapshot for the benchmark and its dependencies. Clean the affected Native modules when switching Scala Native versions. Run it on a quiet machine and compare results on the same hardware. On Linux, `taskset -c 4` before the executable pins it to a logical CPU. These are simple microbenchmarks, not JMH; they do not measure allocations or replace end-to-end HTTP measurements.

## JVM (JMH)

`microbenchmarks/jvm` (module `jvm-jmh`) uses the Deder JMH plugin to measure JVM hot paths in time and allocation:

- `GzipBenchmark`: `GzipHandler` at level 3, compared with the uncompressed response and with deflating on a reused `Deflater`.
- `BodyReadBenchmark`: `Request.bodyString` on a body stream that, like Potateh 0.6.0's, does not override `readAllBytes`.
- `ToResponseBenchmark`: Sharaf-to-Potateh response conversion.

```sh
deder exec -t jmhList -m jvm-jmh
deder exec -t jmhRun -m jvm-jmh -- -bm avgt -tu ns -wi 3 -i 5 -w 1s -r 1s -f 2 -prof gc '.*GzipBenchmark.*'
```

Compare `ns/op` and `gc.alloc.rate.norm` (bytes per operation) before and after a change, on the same machine. Native zlib memory does not appear in `gc.alloc.rate.norm`.
