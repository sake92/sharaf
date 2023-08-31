


```sh


./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.0.5"
git tag -a $VERSION -m "Improve paths handling"
git push origin $VERSION
```

# TODOs

- cookies ?
- adapt query params to requests lib
- add Docker / Watchtower example




