


```sh


./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.0.4"
git tag -a $VERSION -m "Improve error handling"
git push origin $VERSION
```

# TODOs

- cookies ?

- add Docker / Watchtower example




