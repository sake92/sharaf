


```sh


./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.0.3"
git tag -a $VERSION -m "Fix resource serving"
git push origin $VERSION
```

# TODOs

- cookies