


```sh


./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.5.1"
git tag -a $VERSION -m "Fix stuff"
git push origin $VERSION
```

# TODOs

- remove validation or replace with https://jap-company.github.io/fields/docs/overview/
