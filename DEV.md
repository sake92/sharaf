
```sh

./mill clean

./mill __.reformat

./mill __.test

scala-cli compile examples\scala-cli

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.4.0"
git commit --allow-empty -m "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION


```

# TODOs

- MiMa bin compat

- giter8 template for REST
- add more validators https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html
- webjars
