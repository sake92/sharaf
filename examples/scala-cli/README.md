
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

- [GraalVM Native Image with Undertow](hello-undertow-graalvm.scala)
- [Scala Native with http4s/Ember](hello-http4s-native.scala)

Both examples demonstrate plain-text and JSON routes, including JSON input and error handling.
See the [measured footprint](../../docs/content/reference/native-footprint.md) for local size, startup, and memory figures.

Keep `graalvm-resources/` alongside the Undertow example: its metadata registers the generated Undertow/XNIO loggers,
HTTP header fields, direct-buffer cleanup, and version resources required by Native Image.
