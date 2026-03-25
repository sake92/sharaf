
```sh

deder clean

deder -t test

deder exec -t runMvnApp fmt

deder exec -t test

scala compile examples/scala-cli
scala compile examples/htmx

deder exec -t run -m examples-api

# for local dev/test
deder exec -t publishLocal

# RELEASE
./scripts/release.sh 0.16.0

```

