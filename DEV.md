
```sh

./mill clean

./mill -i mill.scalalib.scalafmt/

./mill __.test

scala-cli compile examples\scala-cli

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal
```

```sh

# RELEASE
$VERSION="0.13.0"
git commit --allow-empty -m "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push --atomic origin main $VERSION

```

