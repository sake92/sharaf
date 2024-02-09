
```sh

./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.0.22"
git commit --allow-empty -m "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION


```

# TODOs

- HTMX headers consts

- giter8 template for REST
- add more validators https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html
- webjars
