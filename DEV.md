


```sh


./mill clean

./mill __.reformat

./mill __.test

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

git tag -a 0.5.1 -m "Fix stuff"
git push origin 0.5.1
```

# TODOs

- remove validation or replace with https://jap-company.github.io/fields/docs/overview/
