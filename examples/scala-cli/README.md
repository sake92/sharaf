
Example implementations of https://htmx.org/examples/

Run any of these from this folder:
```sh
scala hello.sc --resource-dir resources
```

If you want to restart the server when files change, just add the `--restart` flag:
```sh
scala hello.sc  --resource-dir resources --restart
```






Authorization examples progress from authentication to ABAC and scoped memberships in
[authorization/](authorization/README.md), with a self-contained HTTP integration suite.

Native executable examples (build and run commands are at the top of each file):

- [Scala Native with http4s/Ember](hello-http4s-native.scala)
- [GraalVM Native Image with JDK HTTP server](hello-jdk-graalvm.scala), including JSON requests and responses

Both examples share plain-text and JSON routes, including JSON input and error handling. A JVM Undertow baseline is
in [hello-undertow.scala](hello-undertow.scala). Follow the [measurement instructions](../../benchmarks/native/README.md)
to build and compare the packaged JVM and native applications.
