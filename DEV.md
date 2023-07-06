


```sh


./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.0.1"
git tag -a $VERSION -m "First release"
git push origin $VERSION
```

# TODOs

- cookies